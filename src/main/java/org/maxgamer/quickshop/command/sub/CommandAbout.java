package org.maxgamer.quickshop.command.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.JavaUtils;
import cc.bukkit.shop.Shop;


public class CommandAbout extends QuickShopCommand {
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    QuickShop.instance();
    sender.sendMessage(
        ChatColor.AQUA + "QuickShop Rev");
    sender.sendMessage(ChatColor.AQUA + "Ver " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
        + Shop.getVersion());
    if (Shop.getVersion().toUpperCase().contains("LTS")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + Shop.getLocaleManager().get("updatenotify.label.lts"));
    } else if (Shop.getVersion().toUpperCase().contains("STABLE")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + Shop.getLocaleManager().get("updatenotify.label.stable"));
    } else if (Shop.getVersion().toUpperCase().contains("QV")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + Shop.getLocaleManager().get("updatenotify.label.qualityverifyed"));
    } else if (Shop.getVersion().toUpperCase().contains("BETA")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + Shop.getLocaleManager().get("updatenotify.label.unstable"));
    } else if (Shop.getVersion().toUpperCase().contains("ALPHA")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + Shop.getLocaleManager().get("updatenotify.label.unstable"));
    } else if (Shop.getVersion().toUpperCase().contains("EARLY ACCESS")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + Shop.getLocaleManager().get("updatenotify.label.unstable"));
    } else if (Shop.getVersion().toUpperCase().contains("SNAPSHOT")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + Shop.getLocaleManager().get("updatenotify.label.unstable"));
    } else {
      sender.sendMessage(
          ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN + "[Main Line]");
    }
    sender.sendMessage(ChatColor.AQUA + "Dev " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
        + JavaUtils.list2String(QuickShop.instance().getDescription().getAuthors()));
    sender.sendMessage(ChatColor.GOLD + "Powered by Bukkit Common Studio");
    sender.sendMessage(ChatColor.RED + "Made with ❤");
  }
}
