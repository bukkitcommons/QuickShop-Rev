package org.maxgamer.quickshop.command;

import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import cc.bukkit.shop.Shop;
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
    
    if (!command.onTabComplete(Bukkit.getConsoleSender(), command.label(), new String[0]).isEmpty())
      completers.add(command);
  }
  
  private void sendHelp(@NotNull CommandSender sender, @NotNull String commandLabel) {
    boolean player = sender instanceof Player;
    sender.sendMessage(Shop.getLocaleManager().get("command.description.title", player ? (Player) sender : null));

    for (CommandData container : commands) {
      if (container.hidden())
        continue;
      
      for (String permission : container.permissions())
        if (!QuickShopPermissionManager.instance().has(sender, permission))
          continue;

      sender.sendMessage(
          ChatColor.GREEN + "/" + commandLabel + " " + container.label() + ChatColor.YELLOW
              + " - " + Shop.getLocaleManager().get("command.description." + container.label(), player ? (Player) sender : null));
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
        if (!QuickShopPermissionManager.instance().has(sender, permission)) {
          sender.sendMessage(Shop.getLocaleManager().get("no-permission", sender instanceof Player ? (Player) sender : null));
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
    if (args.length == 0) {
      return null;
    }
    
    /*
     * Sub commands
     */
    String subLabel = args[0].toLowerCase();
    if (args.length == 1) {
      List<String> tabCommands = Lists.newArrayList();
      for (CommandData data : commands) {
        if (data.hidden()) continue;
        
        for (String permission : data.permissions()) {
          if (!QuickShopPermissionManager.instance().has(sender, permission))
            continue;
        }
        
        if (data.label().startsWith(subLabel)) tabCommands.add(data.label());
      }
      return tabCommands;
    }
    
    /*
     * Command arguments
     */
    for (CommandData container : completers) {
      if (!container.label().startsWith(subLabel))
        continue;
      
      for (String permission : container.permissions()) {
        if (!QuickShopPermissionManager.instance().has(sender, permission))
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
