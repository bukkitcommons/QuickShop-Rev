package org.maxgamer.quickshop.command.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;

public class CommandAbout extends QuickShopCommand {
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    QuickShop.instance();
    sender.sendMessage(
        ChatColor.AQUA + "QuickShop " + ChatColor.YELLOW + QuickShop.getFork());
    sender.sendMessage(ChatColor.AQUA + "Ver " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
        + QuickShop.instance().getVersion());
    if (QuickShop.instance().getVersion().toUpperCase().contains("LTS")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + MsgUtil.getMessage("updatenotify.label.lts", sender));
    } else if (QuickShop.instance().getVersion().toUpperCase().contains("STABLE")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + MsgUtil.getMessage("updatenotify.label.stable", sender));
    } else if (QuickShop.instance().getVersion().toUpperCase().contains("QV")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + MsgUtil.getMessage("updatenotify.label.qualityverifyed", sender));
    } else if (QuickShop.instance().getVersion().toUpperCase().contains("BETA")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + MsgUtil.getMessage("updatenotify.label.unstable", sender));
    } else if (QuickShop.instance().getVersion().toUpperCase().contains("ALPHA")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + MsgUtil.getMessage("updatenotify.label.unstable", sender));
    } else if (QuickShop.instance().getVersion().toUpperCase().contains("EARLY ACCESS")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + MsgUtil.getMessage("updatenotify.label.unstable", sender));
    } else if (QuickShop.instance().getVersion().toUpperCase().contains("SNAPSHOT")) {
      sender.sendMessage(ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
          + MsgUtil.getMessage("updatenotify.label.unstable", sender));
    } else {
      sender.sendMessage(
          ChatColor.AQUA + "Release " + ChatColor.YELLOW + ">> " + ChatColor.GREEN + "[Main Line]");
    }
    sender.sendMessage(ChatColor.AQUA + "Dev " + ChatColor.YELLOW + ">> " + ChatColor.GREEN
        + Util.list2String(QuickShop.instance().getDescription().getAuthors()));
    sender.sendMessage(ChatColor.GOLD + "Powered by Bukkit Common Studio");
    sender.sendMessage(ChatColor.RED + "Made with ❤");
  }
}