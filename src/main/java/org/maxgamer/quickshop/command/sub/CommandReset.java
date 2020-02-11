package org.maxgamer.quickshop.command.sub;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import lombok.SneakyThrows;

public class CommandReset extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.reset");
  }
  
  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    ArrayList<String> tab = new ArrayList<>();
    tab.add("lang");
    tab.add("config");
    tab.add("messages");
    return tab;
  }

  @Override
  @SneakyThrows
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {

    if (cmdArg.length < 1) {
      sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("command.no-type-given", sender));
      return;
    }

    switch (cmdArg[0]) {
      case "lang":
        QuickShop.instance().getLocaleManager().load();
        sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("complete", sender));
        break;
      case "config":
        QuickShop.instance().saveDefaultConfig();
        QuickShop.instance().reloadConfig();
        Bukkit.getPluginManager().disablePlugin(QuickShop.instance());
        Bukkit.getPluginManager().enablePlugin(QuickShop.instance());
        sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("complete", sender));
        break;
      case "messages":
        QuickShop.instance().getLocaleManager().load();
        sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("complete", sender));
        break;
    }
  }
}
