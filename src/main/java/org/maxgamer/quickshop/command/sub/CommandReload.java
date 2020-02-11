package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;


public class CommandReload extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.reload");
  }
  
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("command.reloading", sender));
    Bukkit.getPluginManager().disablePlugin(QuickShop.instance());
    Bukkit.getPluginManager().enablePlugin(QuickShop.instance());
  }
}
