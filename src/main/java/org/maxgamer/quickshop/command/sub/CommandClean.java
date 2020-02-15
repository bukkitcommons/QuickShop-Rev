package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.shop.ContainerQuickShop;
import org.maxgamer.quickshop.shop.QuickShopLoader;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.ShopUtils;
import com.google.common.collect.Lists;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopLocation;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandClean extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.clean");
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
    boolean force = cmdArg.length > 0 && cmdArg[0].equalsIgnoreCase("force");
    
    if (sender instanceof Player)
      handleCleanShops(((Player) sender).getWorld(), sender, force);
    else
      Bukkit.getWorlds().forEach(world -> handleCleanShops(world, sender, force));
  }
  
  private static void handleCleanShops(World world, CommandSender sender, boolean force) {
    sender.sendMessage(Shop.getLocaleManager().get("command.cleaning", sender));

    List<ContainerShop> pendingRemoval = Lists.newArrayList();
    int[] count = {0, 0};
    
    Shop.getLoader().getAllShops().forEach(data -> {
      try {
        ContainerShop shop;
        ShopViewer viewer = Shop.getManager().getLoadedShopAt(data.location());
        // Do not create shop instance for loaded shop to avoid dupe display
        shop = viewer.isPresent() ? viewer.get() :
          new ContainerQuickShop(
            ShopLocation.from(world, data.x(), data.y(), data.z()),
            data.price(), ItemUtils.deserialize(data.item()),
            data.moderators(), data.unlimited(), data.type(), false);
        
        if (data.type() == ShopType.SELLING && shop.getRemainingStock() == 0) {
          if (!ShopUtils.canBeShopIgnoreBlocklist(shop.getLocation().block().getState())) {
            pendingRemoval.add(shop);
            return;
          }
          
          if (!force) {
            ItemStack[] contents =
                ((InventoryHolder) shop.getLocation().block().getState()).getInventory().getContents();
            
            for (ItemStack i : contents)
              if (i != null) {
                count[1]++;
                return; // Next shop
              }
          }
          
          ContainerQuickShop cs = (ContainerQuickShop) shop;
          if (!cs.isDualShop()) {
            pendingRemoval.add(shop);
            count[0]++;
          }
        }
      } catch (IllegalStateException | InvalidConfigurationException e) {
        e.printStackTrace();
        //pendingRemoval.add(shop); // The shop is not there anymore, remove it
      }
    });

    for (ContainerShop shop : pendingRemoval)
      QuickShopLoader.instance().delete(shop);

    Shop.getMessager().clean();
    sender.sendMessage(Shop.getLocaleManager().get("command.cleaned", sender, "" + count[0]));
    
    if (!force && count[1] > 0)
      sender.sendMessage(
          count[1] +
          " out-of-stock shops have non-goods items inside and have been ignored," +
          " use '/qs clean force' to forcefully clean them," +
          " by that, items in the chests will no longer be protected by QuickShop.");
  }
}
