package org.maxgamer.quickshop.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.command.SneakyTabs;
import org.maxgamer.quickshop.shop.ContainerShop;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

public class SubCommand_SilentEmpty extends SneakyTabs implements CommandProcesser {

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (cmdArg.length < 4) {
      Util.debug("Exception on command, cancel.");
      return;
    }

    final ShopViewer shop =
        ShopManager.instance().getLoadedShopAt(new Location(Bukkit.getWorld(cmdArg[0]),
            Integer.parseInt(cmdArg[1]), Integer.parseInt(cmdArg[2]), Integer.parseInt(cmdArg[3])));

    if (!(shop.get() instanceof ContainerShop)) {
      sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
      return;
    }

    final ContainerShop cs = (ContainerShop) shop.get();
    final Inventory inventory = cs.getInventory();

    if (inventory == null) {
      // TODO: 24/11/2019 Send message about that issue.
      return;
    }

    inventory.clear();
    MsgUtil.sendControlPanelInfo(sender, shop.get());
    sender.sendMessage(MsgUtil.getMessage("empty-success", sender));
  }
}
