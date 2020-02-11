package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
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
      sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("not-looking-at-shop", sender));
      return;
    }

    shop.get().setUnlimited(!shop.get().isUnlimited());
    // shop.setSignText();
    shop.get().save();
    QuickShop.instance().getLocaleManager().sendControlPanelInfo(sender, shop.get());

    if (shop.get().isUnlimited()) {
      sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("command.toggle-unlimited.unlimited", sender));
      return;
    }

    sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("command.toggle-unlimited.limited", sender));
  }
}
