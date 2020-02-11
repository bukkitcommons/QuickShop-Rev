package org.maxgamer.quickshop.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.sub.CommandAbout;
import org.maxgamer.quickshop.command.sub.CommandAmount;
import org.maxgamer.quickshop.command.sub.CommandBuy;
import org.maxgamer.quickshop.command.sub.CommandClean;
import org.maxgamer.quickshop.command.sub.CommandCleanGhost;
import org.maxgamer.quickshop.command.sub.CommandCreate;
import org.maxgamer.quickshop.command.sub.CommandDebug;
import org.maxgamer.quickshop.command.sub.CommandEmpty;
import org.maxgamer.quickshop.command.sub.CommandFetchMessage;
import org.maxgamer.quickshop.command.sub.CommandFind;
import org.maxgamer.quickshop.command.sub.CommandHelp;
import org.maxgamer.quickshop.command.sub.CommandInfo;
import org.maxgamer.quickshop.command.sub.CommandPaste;
import org.maxgamer.quickshop.command.sub.CommandPrice;
import org.maxgamer.quickshop.command.sub.CommandRefill;
import org.maxgamer.quickshop.command.sub.CommandReload;
import org.maxgamer.quickshop.command.sub.CommandRemove;
import org.maxgamer.quickshop.command.sub.CommandReset;
import org.maxgamer.quickshop.command.sub.CommandSell;
import org.maxgamer.quickshop.command.sub.CommandSetOwner;
import org.maxgamer.quickshop.command.sub.CommandSilentBuy;
import org.maxgamer.quickshop.command.sub.CommandSilentEmpty;
import org.maxgamer.quickshop.command.sub.CommandSilentPreview;
import org.maxgamer.quickshop.command.sub.CommandSilentRemove;
import org.maxgamer.quickshop.command.sub.CommandSilentSell;
import org.maxgamer.quickshop.command.sub.CommandSilentUnlimited;
import org.maxgamer.quickshop.command.sub.CommandStaff;
import org.maxgamer.quickshop.command.sub.CommandSuperCreate;
import org.maxgamer.quickshop.command.sub.CommandUnlimited;
import org.maxgamer.quickshop.command.sub.CommandUpdate;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.command.CommandData;
import lombok.Data;

@Data
public class QuickShopCommandManager implements TabCompleter, CommandExecutor {
  private Set<CommandData> cmds = new HashSet<>();
  private CommandData rootContainer = null;//CommandData.builder().prefix(null).permission(null)
      //.executor(new CommandQuickShop()).build();

  public QuickShopCommandManager() {
    /*
    registerCmd(CommandData.builder().prefix("help").permission(null)
        .executor(new CommandHelp()).build());
    registerCmd(CommandData.builder().prefix("unlimited").permission("quickshop.unlimited")
        .executor(new CommandUnlimited()).build());
    registerCmd(CommandData.builder().prefix("silentunlimited").hidden(true)
        .permission("quickshop.unlimited").executor(new CommandSilentUnlimited()).build());
    registerCmd(CommandData.builder().prefix("slientunlimited").hidden(true)
        .permission("quickshop.unlimited").executor(new CommandSilentUnlimited()).build());
    registerCmd(CommandData.builder().prefix("setowner").permission("quickshop.setowner")
        .executor(new CommandSetOwner()).build());
    registerCmd(CommandData.builder().prefix("owner").hidden(true)
        .permission("quickshop.setowner").executor(new CommandSetOwner()).build());
    registerCmd(CommandData.builder().prefix("sell").permission("quickshop.create.sell")
        .executor(new CommandSell()).build());
    registerCmd(CommandData.builder().prefix("silentbuy").hidden(true)
        .permission("quickshop.create.buy").executor(new CommandSilentBuy()).build());
    registerCmd(CommandData.builder().prefix("silentsell").hidden(true)
        .permission("quickshop.create.sell").executor(new CommandSilentSell()).build());
    registerCmd(CommandData.builder().prefix("price")
        .permission("quickshop.create.changeprice").executor(new CommandPrice()).build());
    registerCmd(CommandData.builder().prefix("remove").permission(null)
        .executor(new CommandRemove()).build());
    registerCmd(CommandData.builder().prefix("silentremove").hidden(true).permission(null)
        .executor(new CommandSilentRemove()).build());
    registerCmd(CommandData.builder().prefix("empty").permission("quickshop.empty")
        .executor(new CommandEmpty()).build());
    registerCmd(CommandData.builder().prefix("refill").permission("quickshop.refill")
        .executor(new CommandRefill()).build());
    registerCmd(CommandData.builder().prefix("silentempty").hidden(true)
        .permission("quickshop.empty").executor(new CommandSilentEmpty()).build());
    registerCmd(CommandData.builder().prefix("silentpreview").hidden(true)
        .permission("quickshop.preview").executor(new CommandSilentPreview()).build());
    registerCmd(CommandData.builder().prefix("clean").permission("quickshop.clean")
        .executor(new CommandClean()).build());
    registerCmd(CommandData.builder().prefix("reload").permission("quickshop.reload")
        .executor(new CommandReload()).build());
    registerCmd(CommandData.builder().prefix("debug").permission("quickshop.debug")
        .executor(new CommandDebug()).build());
    registerCmd(CommandData.builder().prefix("fetchmessage")
        .permission("quickshop.fetchmessage").executor(new CommandFetchMessage()).build());
    registerCmd(CommandData.builder().prefix("info").permission("quickshop.info")
        .executor(new CommandInfo()).build());
    registerCmd(CommandData.builder().prefix("paste").permission("quickshop.paste")
        .executor(new CommandPaste()).build());
    registerCmd(CommandData.builder().prefix("staff").permission("quickshop.staff")
        .executor(new CommandStaff()).build());
    registerCmd(CommandData.builder().prefix("create").permission("quickshop.create.cmd")
        .permission("quickshop.create.sell").executor(new CommandCreate()).build());
    registerCmd(CommandData.builder().prefix("update").hidden(true)
        .permission("quickshop.alert").executor(new CommandUpdate()).build());
    registerCmd(CommandData.builder().prefix("find").permission("quickshop.find")
        .executor(new CommandFind()).build());
    registerCmd(
        CommandData.builder().prefix("supercreate").permission("quickshop.create.admin")
            .permission("quickshop.create.sell").executor(new CommandSuperCreate()).build());
    registerCmd(CommandData.builder().prefix("cleanghost").permission("quickshop.cleanghost")
        .hidden(true).executor(new CommandCleanGhost()).build());
    registerCmd(CommandData.builder().prefix("reset").hidden(true)
        .permission("quickshop.reset").executor(new CommandReset()).build());
    */
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String commandLabel, @NotNull String[] cmdArg) {
    if (sender instanceof Player) {
      if (BaseConfig.tabCompleteSound) {
        Player player = (Player) sender;
        ((Player) sender).playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 80.0F,
            1.0F);
      }
    }

    String[] passthroughArgs;
    if (cmdArg.length != 0) {
      passthroughArgs = new String[cmdArg.length - 1];
      System.arraycopy(cmdArg, 1, passthroughArgs, 0, passthroughArgs.length);
    } else {
      passthroughArgs = new String[0];
      Util.debug("Print help cause no args (/qs)");
      rootContainer.executor().onCommand(sender, commandLabel, passthroughArgs);
      return true;
    }
    // if (cmdArg.length == 0)
    // return rootContainer.executor().onCommand(sender, commandLabel, temp);
    for (CommandData container : cmds) {
      if (!container.label().equalsIgnoreCase(cmdArg[0])) {
        continue;
      }
      List<String> requirePermissions = container.permissions();
      if (container.permissions() != null) {
        for (String requirePermission : requirePermissions) {

          if (requirePermission != null && !requirePermission.isEmpty()
              && !PermissionManager.instance().has(sender, requirePermission)) {
            Util.debug(
                "Sender " + sender.getName() + " trying execute the command: " + commandLabel + " "
                    + Util.array2String(cmdArg) + ", but no permission " + requirePermission);
            sender.sendMessage(MsgUtil.getMessage("no-permission", sender));
            return true;
          }
        }
      }
      Util.debug("Execute container: " + container.label() + " - " + cmdArg[0]);
      container.executor().onCommand(sender, commandLabel, passthroughArgs);
      return true;
    }
    Util.debug("All checks failed, print helps");
    rootContainer.executor().onCommand(sender, commandLabel, passthroughArgs);
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
      @NotNull Command command, @NotNull String commandLabel, @NotNull String[] cmdArg) {
    // No args, it shouldn't happend
    if (QuickShop.instance().getBootError() != null) {
      return null;
    }
    if (sender instanceof Player) {
      if (BaseConfig.tabCompleteSound) {
        Player player = (Player) sender;
        ((Player) sender).playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 80.0F, 1.0F);
      }
    }
    if (cmdArg.length == 0 || cmdArg.length == 1) {
      // No args
      return getRootContainer().executor().onTabComplete(sender, commandLabel, cmdArg);
    }
    // Main args/more args
    String[] passthroughArgs;
    passthroughArgs = new String[cmdArg.length - 1];
    System.arraycopy(cmdArg, 1, passthroughArgs, 0, passthroughArgs.length);
    for (CommandData container : cmds) {
      if (!container.label().toLowerCase().startsWith(cmdArg[0])) {
        continue;
      }
      List<String> requirePermissions = container.permissions();
      if (container.permissions() != null) {
        for (String requirePermission : requirePermissions) {
          if (requirePermission != null && !requirePermission.isEmpty()
              && !PermissionManager.instance().has(sender, requirePermission)) {
            Util.debug(
                "Sender " + sender.getName() + " trying tab-complete the command: " + commandLabel
                    + " " + Util.array2String(cmdArg) + ", but no permission " + requirePermission);
            return null;
          }
        }
      }
      Util.debug("Execute container: " + container.label());
      return container.executor().onTabComplete(sender, commandLabel, passthroughArgs);
    }

    return null;
  }

  private void registerCmd(CommandData container) {
    cmds.add(container);
  }
}
