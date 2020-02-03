package org.maxgamer.quickshop.command.sub;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.command.SneakyTabs;
import org.maxgamer.quickshop.shop.ContainerShop;
import org.maxgamer.quickshop.shop.ItemPreviewer;
import org.maxgamer.quickshop.utils.MsgUtil;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

public class SubCommand_SilentPreview extends SneakyTabs implements CommandProcesser {
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Can't run this command from Console");
      return;
    }

    if (cmdArg.length < 4) {
      Util.debugLog("Exception on command, cancel.");
      return;
    }

    final ShopViewer shop =
        QuickShop.instance().getShopManager().getShop(new Location(QuickShop.instance().getServer().getWorld(cmdArg[0]),
            Integer.parseInt(cmdArg[1]), Integer.parseInt(cmdArg[2]), Integer.parseInt(cmdArg[3])));

    if (!(shop.get() instanceof ContainerShop)) {
      sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
      return;
    }

    new ItemPreviewer(shop.get().getItem(), (Player) sender).show();
  }
}
