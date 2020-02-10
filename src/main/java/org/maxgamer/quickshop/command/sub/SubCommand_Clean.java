package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.command.SneakyTabs;
import org.maxgamer.quickshop.shop.ContainerShop;
import org.maxgamer.quickshop.shop.ShopLoader;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopType;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;

public class SubCommand_Clean extends SneakyTabs implements CommandProcesser {

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (sender instanceof ConsoleCommandSender) {
      sender.sendMessage("Can't run this command by Console");
      return;
    }

    sender.sendMessage(MsgUtil.getMessage("command.cleaning", sender));

    final ArrayList<Shop> pendingRemoval = new java.util.ArrayList<>();
    int[] count = {0, 0};
    
    ShopLoader.instance().getAllShops().forEach(shop -> {
      try {
        if (shop.getLocation().world() != null && shop.is(ShopType.SELLING)
            && shop.getRemainingStock() == 0 && shop instanceof ContainerShop) {
          // FIXME load air shop
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
          
          ContainerShop cs = (ContainerShop) shop;
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

    for (Shop shop : pendingRemoval)
      ShopLoader.instance().delete(shop);

    MsgUtil.clean();
    sender.sendMessage(MsgUtil.getMessage("command.cleaned", sender, "" + count[0]));
    sender.sendMessage("There is " + count[1] + " shops with non-goods items inside have been ignored.");
  }
}
