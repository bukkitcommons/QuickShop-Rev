package org.maxgamer.quickshop.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.command.SneakyTabs;

public class SubCommand_Reload extends SneakyTabs implements CommandProcesser {
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    sender.sendMessage(MsgUtil.getMessage("command.reloading", sender));
    Bukkit.getPluginManager().disablePlugin(QuickShop.instance());
    Bukkit.getPluginManager().enablePlugin(QuickShop.instance());
  }
}
