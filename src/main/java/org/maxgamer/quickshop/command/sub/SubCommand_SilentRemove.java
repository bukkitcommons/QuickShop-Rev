package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.command.SneakyTabs;
import org.maxgamer.quickshop.shop.ShopLoader;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

public class SubCommand_SilentRemove extends SneakyTabs implements CommandProcesser {

  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    return new ArrayList<>();
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (cmdArg.length < 4) {
      Util.debugLog("Exception on command, cancel.");
      return;
    }

    final Player p = (Player) sender;
    final ShopViewer shop =
        ShopManager.instance().getLoadedShopAt(new Location(Bukkit.getWorld(cmdArg[0]),
            Integer.parseInt(cmdArg[1]), Integer.parseInt(cmdArg[2]), Integer.parseInt(cmdArg[3])));

    if (!shop.isPresent()) {
      sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
      return;
    }

    if (!shop.get().getModerator().isModerator(p.getUniqueId())
        && !QuickShop.getPermissionManager().hasPermission(sender, "quickshop.other.destroy")) {
      sender.sendMessage(ChatColor.RED + MsgUtil.getMessage("no-permission", sender));
      return;
    }

    ShopLoader.instance().delete(shop.get());
  }
}
