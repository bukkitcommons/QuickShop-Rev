package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.command.CommandProcesser;


public class CommandReload extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.reload");
  }
  
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    sender.sendMessage(MsgUtil.getMessage("command.reloading", sender));
    Bukkit.getPluginManager().disablePlugin(QuickShop.instance());
    Bukkit.getPluginManager().enablePlugin(QuickShop.instance());
  }
}
