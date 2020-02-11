package org.maxgamer.quickshop.command.sub;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.command.CommandData;

public class CommandHelp extends QuickShopCommand {
  
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    sendHelp(sender, commandLabel);
  }

  private void sendHelp(@NotNull CommandSender s, @NotNull String commandLabel) {
    s.sendMessage(MsgUtil.getMessage("command.description.title", s));

    for (CommandData container : ((QuickShop) Shop.instance()).getCommandManager().getCmds()) {
      final List<String> requirePermissions = container.permissions();

      if (requirePermissions != null && !requirePermissions.isEmpty()) {
        for (String requirePermission : requirePermissions) {
          // FIXME: 24/11/2019 You are already checked the null and empty
          if (requirePermission != null && !requirePermission.isEmpty()
              && !PermissionManager.instance().has(s, requirePermission)) {
            // noinspection UnnecessaryContinue
            continue;
          }
        }
      }

      if (container.hidden()) {
        continue;
      }

      s.sendMessage(
          ChatColor.GREEN + "/" + commandLabel + " " + container.label() + ChatColor.YELLOW
              + " - " + MsgUtil.getMessage("command.description." + container.label(), s));
    }
  }
}
