package org.maxgamer.quickshop.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.sub.CommandHelp;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Lists;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.command.CommandData;
import cc.bukkit.shop.command.ShopCommand;

public class CommandQuickShop implements ShopCommand {
  // FIXME Merge this with manager
  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] strings) {
    final List<String> candidate = Lists.newArrayList();

    for (CommandData container : ((QuickShop) Shop.instance()).getCommandManager().getCmds()) { // FIXME api
      if (!container.label().startsWith(strings[0])
          && !container.label().equals(strings[0])) {
        continue;
      }

      final List<String> requirePermissions = container.permissions();

      if (requirePermissions != null) {
        for (String requirePermission : requirePermissions) {
          if (requirePermission != null && !requirePermission.isEmpty()
              && !PermissionManager.instance().has(sender, requirePermission)) {
            Util.debug("Sender " + sender.getName() + " trying tab-complete the command: "
                + commandLabel + ", but no permission " + requirePermission);
            return new ArrayList<>();
          }
        }
      }

      if (!container.hidden()) {
        candidate.add(container.label());
      }
    }

    return candidate;
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    new CommandHelp().onCommand(sender, commandLabel, cmdArg);
  }

  @Override
  public boolean hidden() {
    return false;
  }

  @Override
  public List<String> permissions() {
    return Collections.emptyList();
  }

  @Override
  public String label() {
    return "";
  }
}
