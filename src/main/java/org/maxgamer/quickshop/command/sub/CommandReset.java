package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import cc.bukkit.shop.Shop;
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
      sender.sendMessage(Shop.getLocaleManager().getMessage("command.no-type-given", sender));
      return;
    }

    switch (cmdArg[0]) {
      case "lang":
        Shop.getLocaleManager().load();
        sender.sendMessage(Shop.getLocaleManager().getMessage("complete", sender));
        break;
      case "config":
        QuickShop.instance().reloadConfig();
        Bukkit.getPluginManager().disablePlugin(QuickShop.instance());
        Bukkit.getPluginManager().enablePlugin(QuickShop.instance());
        sender.sendMessage(Shop.getLocaleManager().getMessage("complete", sender));
        break;
      case "messages":
        Shop.getLocaleManager().load();
        sender.sendMessage(Shop.getLocaleManager().getMessage("complete", sender));
        break;
    }
  }
}
