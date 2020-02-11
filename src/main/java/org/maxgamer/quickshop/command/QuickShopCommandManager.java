package org.maxgamer.quickshop.command;

import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import cc.bukkit.shop.command.CommandData;
import cc.bukkit.shop.command.ShopCommand;

public class QuickShopCommandManager implements TabCompleter, CommandExecutor {
  private Set<CommandData> commands = Sets.newHashSet();
  private Set<CommandData> completers = Sets.newHashSet();
  
  public QuickShopCommandManager() {
    registerCommands();
  }
  
  public class CommandHelp extends QuickShopCommand {
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
      sendHelp(sender, commandLabel);
    }
  }
  
  public void registerCommands() {
    registerCommand(new CommandHelp());
    
    try {
      ClassPath classPath = ClassPath.from(QuickShopCommand.class.getClassLoader());
      Set<ClassInfo> info = classPath.getTopLevelClasses("org.maxgamer.quickshop.command.sub");
      
      info.forEach(clazz -> {
        if (clazz.getSimpleName().startsWith("Command")) {
          try {
            Object command = Class.forName(clazz.getName()).newInstance();
            if (command instanceof ShopCommand)
              registerCommand((ShopCommand) command);
            
          } catch (Throwable t) {
            t.printStackTrace();
          }
        }
      });
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
  
  public void registerCommand(@NotNull ShopCommand command) {
    commands.add(command);
    
    if (!command.onTabComplete(null, null, null).isEmpty())
      completers.add(command);
  }
  
  private void sendHelp(@NotNull CommandSender sender, @NotNull String commandLabel) {
    sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("command.description.title", sender));

    for (CommandData container : commands) {
      if (container.hidden())
        continue;
      
      for (String permission : container.permissions())
        if (!PermissionManager.instance().has(sender, permission))
          continue;

      sender.sendMessage(
          ChatColor.GREEN + "/" + commandLabel + " " + container.label() + ChatColor.YELLOW
              + " - " + QuickShop.instance().getLocaleManager().getMessage("command.description." + container.label(), sender));
    }
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (BaseConfig.tabCompleteSound && sender instanceof Player) {
      Player player = (Player) sender;
      player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 80.0F, 1.0F);
    }

    if (args.length == 0) {
      sendHelp(sender, label);
      return true;
    }
    
    String subLabel = args[0];
    for (CommandData container : commands) {
      if (!container.label().equalsIgnoreCase(subLabel))
        continue;
      
      for (String permission : container.permissions()) {
        if (!PermissionManager.instance().has(sender, permission)) {
          sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("no-permission", sender));
          return true;
        }
      }
      
      String[] passthroughArgs = new String[args.length - 1];
      System.arraycopy(args, 1, passthroughArgs, 0, passthroughArgs.length);
      
      Util.debug("Execute command: " + container.label() + " - " + subLabel);
      container.executor().onCommand(sender, subLabel, passthroughArgs);
      return true;
    }
    
    sendHelp(sender, label);
    return true;
  }
  
  @Nullable
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (args.length == 0 || args.length == 1) {
      return null;
    }
    
    String subLabel = args[0].toLowerCase();
    for (CommandData container : completers) {
      if (!container.label().startsWith(subLabel))
        continue;
      
      for (String requirePermission : container.permissions()) {
        if (!PermissionManager.instance().has(sender, requirePermission))
          return null;
      }
      
      String[] passthroughArgs = new String[args.length - 1];
      System.arraycopy(args, 1, passthroughArgs, 0, passthroughArgs.length);
      
      Util.debug("Execute compeletor: " + container.label());
      return container.executor().onTabComplete(sender, label, passthroughArgs);
    }
    
    return null;
  }
}
