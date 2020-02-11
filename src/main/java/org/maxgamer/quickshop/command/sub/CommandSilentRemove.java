package org.maxgamer.quickshop.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandSilentRemove extends QuickShopCommand {
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (cmdArg.length < 4) {
      Util.debug("Exception on command, cancel.");
      return;
    }

    final Player p = (Player) sender;
    final ShopViewer shop =
        Shop.getManager().getLoadedShopAt(new Location(Bukkit.getWorld(cmdArg[0]),
            Integer.parseInt(cmdArg[1]), Integer.parseInt(cmdArg[2]), Integer.parseInt(cmdArg[3])));

    if (!shop.isPresent()) {
      sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("not-looking-at-shop", sender));
      return;
    }

    if (!shop.get().getModerator().isModerator(p.getUniqueId())
        && !PermissionManager.instance().has(sender, "quickshop.other.destroy")) {
      sender.sendMessage(ChatColor.RED + QuickShop.instance().getLocaleManager().getMessage("no-permission", sender));
      return;
    }

    Shop.getLoader().delete(shop.get());
  }
}
