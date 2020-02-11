package org.maxgamer.quickshop.command.sub;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.shop.ContainerQuickShop;
import org.maxgamer.quickshop.shop.QuickShopLoader;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import com.google.common.collect.Lists;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.command.SneakyTabs;
import cc.bukkit.shop.data.ShopLocation;

public class SubCommand_Clean extends SneakyTabs implements CommandProcesser {

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (sender instanceof ConsoleCommandSender) {
      sender.sendMessage("Can't run this command by Console");
      return;
    }

    sender.sendMessage(MsgUtil.getMessage("command.cleaning", sender));

    List<ContainerShop> pendingRemoval = Lists.newArrayList();
    int[] count = {0, 0};
    
    Shop.getLoader().getAllShops().forEach(data -> {
      ContainerShop shop = new ContainerQuickShop(
          ShopLocation.from(((Player) sender).getWorld(), data.x(), data.y(), data.z()),
          data.price(), data.item(),
          data.moderators(), data.unlimited(), data.type());
      
      try {
        if (data.type() == ShopType.SELLING && shop.getRemainingStock() == 0) {
          if (!Util.canBeShopIgnoreBlocklist(shop.getLocation().block().getState())) {
            pendingRemoval.add(shop);
            return;
          }
          
          if (cmdArg.length < 1 || !"force".equalsIgnoreCase(cmdArg[0])) {
            ItemStack[] contents =
                ((InventoryHolder) shop.getLocation().block().getState()).getInventory().getContents();
            
            for (ItemStack i : contents)
              if (i != null) {
                count[1]++;
                return;
              }
          }
          
          ContainerQuickShop cs = (ContainerQuickShop) shop;
          if (cs.isDualShop()) {
            return;
          }
          pendingRemoval.add(shop); // Is selling, but has no stock, and is a chest shop, but is not
                                    // a double shop.
          // Can be deleted safely.
          count[0]++;
        }
      } catch (IllegalStateException e) {
        pendingRemoval.add(shop); // The shop is not there anymore, remove it
      }
    });

    for (ContainerShop shop : pendingRemoval)
      QuickShopLoader.instance().delete(shop);

    MsgUtil.clean();
    sender.sendMessage(MsgUtil.getMessage("command.cleaned", sender, "" + count[0]));
    sender.sendMessage("There is " + count[1] + " shops with non-goods items inside have been ignored.");
  }
}
