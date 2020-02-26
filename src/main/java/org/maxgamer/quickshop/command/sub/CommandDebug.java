package org.maxgamer.quickshop.command.sub;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.utils.JavaUtils;
import cc.bukkit.shop.Shop;

public class CommandDebug extends QuickShopCommand {
    @Override
    public List<String> permissions() {
        return Collections.singletonList("quickshop.debug");
    }
    
    @NotNull
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        final ArrayList<String> list = new ArrayList<>();
        
        list.add("debug");
        list.add("dev");
        list.add("devmode");
        list.add("handlerlist");
        list.add("jvm");
        
        return list;
    }
    
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        if (cmdArg.length < 1) {
            switchDebug(sender);
            return;
        }
        
        switch (cmdArg[0]) {
            case "debug":
            case "dev":
            case "devmode":
                switchDebug(sender);
                break;
            case "handlerlist":
                if (cmdArg.length < 2) {
                    sender.sendMessage("You must given a event");
                    break;
                }
                
                printHandlerList(sender, cmdArg[1]);
                break;
            case "jvm":
                RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
                
                List<String> arguments = runtimeMxBean.getInputArguments();
                sender.sendMessage(ChatColor.GOLD + "Arguments: " + ChatColor.AQUA + JavaUtils.list2String(arguments));
                sender.sendMessage(ChatColor.GOLD + "Name: " + ChatColor.AQUA + runtimeMxBean.getName());
                sender.sendMessage(ChatColor.GOLD + "VM Name: " + ChatColor.AQUA + runtimeMxBean.getVmName());
                sender.sendMessage(ChatColor.GOLD + "Uptime: " + ChatColor.AQUA + runtimeMxBean.getUptime());
                sender.sendMessage(ChatColor.GOLD + "JVM Ver: " + ChatColor.AQUA + runtimeMxBean.getVmVersion());
                Map<String, String> sys = runtimeMxBean.getSystemProperties();
                List<String> sysData = new ArrayList<>();
                sys.keySet().forEach(key -> sysData.add(key + "=" + sys.get(key)));
                sender.sendMessage(ChatColor.GOLD + "Sys Pro: " + ChatColor.AQUA + JavaUtils.list2String(sysData));
                break;
            default:
                sender.sendMessage("Error, no correct args given.");
                break;
        }
    }
    
    public void switchDebug(@NotNull CommandSender sender) {
        if (BaseConfig.developerMode) {
            BaseConfig.developerMode = false;
            Shop.instance().saveConfig();
            Bukkit.getPluginManager().disablePlugin(Shop.instance());
            Bukkit.getPluginManager().enablePlugin(Shop.instance());
            sender.sendMessage(Shop.getLocaleManager().get("command.now-nolonger-debuging"));
            return;
        }
        
        BaseConfig.developerMode = true;
        Shop.instance().saveConfig();
        Bukkit.getPluginManager().disablePlugin(Shop.instance());
        Bukkit.getPluginManager().enablePlugin(Shop.instance());
        sender.sendMessage(Shop.getLocaleManager().get("command.now-debuging"));
    }
    
    public void printHandlerList(@NotNull CommandSender sender, String event) {
        try {
            final Class<?> clazz = Class.forName(event);
            final Method method = clazz.getMethod("getHandlerList");
            final Object[] obj = new Object[0];
            final HandlerList list = (HandlerList) method.invoke(null, obj);
            
            for (RegisteredListener listener1 : list.getRegisteredListeners()) {
                sender.sendMessage(ChatColor.AQUA + listener1.getPlugin().getName() + ChatColor.YELLOW + " # " + ChatColor.GREEN + listener1.getListener().getClass().getCanonicalName());
            }
        } catch (Throwable th) {
            sender.sendMessage("ERR " + th.getMessage());
            th.printStackTrace();
        }
    }
}
