package org.maxgamer.quickshop.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.command.SneakyTabs;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

public class SubCommand_SilentUnlimited extends SneakyTabs implements CommandProcesser {
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (cmdArg.length < 4) {
      Util.debugLog("Exception on command, cancel.");
      return;
    }

    final ShopViewer shop =
        ShopManager.instance().getLoadedShopAt(new Location(Bukkit.getWorld(cmdArg[0]),
            Integer.parseInt(cmdArg[1]), Integer.parseInt(cmdArg[2]), Integer.parseInt(cmdArg[3])));

    if (!shop.isPresent()) {
      sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
      return;
    }

    shop.get().setUnlimited(!shop.get().isUnlimited());
    // shop.setSignText();
    shop.get().save();
    MsgUtil.sendControlPanelInfo(sender, shop.get());

    if (shop.get().isUnlimited()) {
      sender.sendMessage(MsgUtil.getMessage("command.toggle-unlimited.unlimited", sender));
      return;
    }

    sender.sendMessage(MsgUtil.getMessage("command.toggle-unlimited.limited", sender));
  }
}
