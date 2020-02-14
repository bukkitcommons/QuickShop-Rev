package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandSilentUnlimited extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.unlimited");
  }
  
  @Override
  public boolean hidden() {
    return true;
  }
  
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

    if (!shop.isPresent()) {
      sender.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop", sender));
      return;
    }

    shop.get().setUnlimited(!shop.get().isUnlimited());
    // shop.setSignText();
    shop.get().save();
    Shop.getLocaleManager().sendControlPanelInfo((@NotNull Player) sender, shop.get());

    if (shop.get().isUnlimited()) {
      sender.sendMessage(Shop.getLocaleManager().get("command.toggle-unlimited.unlimited", sender));
      return;
    }

    sender.sendMessage(Shop.getLocaleManager().get("command.toggle-unlimited.limited", sender));
  }
}
