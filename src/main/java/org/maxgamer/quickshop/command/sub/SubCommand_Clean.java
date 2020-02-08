package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.shop.ContainerShop;
import org.maxgamer.quickshop.shop.ShopLoader;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopType;
import org.maxgamer.quickshop.utils.messages.MsgUtil;

public class SubCommand_Clean implements CommandProcesser {

  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    return new ArrayList<>();
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (sender instanceof Server) {
      sender.sendMessage("Can't run this command by Console");
      return;
    }

    sender.sendMessage(MsgUtil.getMessage("command.cleaning", sender));

    final ArrayList<Shop> pendingRemoval = new java.util.ArrayList<>();
    int[] count = {0};
    
    ShopLoader.instance().forEachShops(shop -> {
      try {
        if (shop.getLocation().world() != null && shop.is(ShopType.SELLING)
            && shop.getRemainingStock() == 0 && shop instanceof ContainerShop) {
          ContainerShop cs = (ContainerShop) shop;
          if (cs.isDoubleShop()) {
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
  }
}
