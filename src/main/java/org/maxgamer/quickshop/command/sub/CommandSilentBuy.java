package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandSilentBuy extends QuickShopCommand {

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (cmdArg.length < 4) {
      Util.debug("Exception on command, cancel.");
      return;
    }

    final ShopViewer shop =
        Shop.getManager().getLoadedShopAt(new Location(Bukkit.getWorld(cmdArg[0]),
            Integer.parseInt(cmdArg[1]), Integer.parseInt(cmdArg[2]), Integer.parseInt(cmdArg[3])));

    if (!shop.isPresent() || !shop.get().getModerator().isModerator(((Player) sender).getUniqueId())) {
      sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
      return;
    }

    shop.get().setShopType(ShopType.BUYING);
    shop.get().setSignText();
    shop.get().save();
    MsgUtil.sendControlPanelInfo(sender, shop.get());
    sender.sendMessage(
        MsgUtil.getMessage("command.now-buying", sender, Util.getItemStackName(shop.get().getItem())));
  }
}
