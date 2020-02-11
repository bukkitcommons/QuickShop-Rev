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
        File item = new File(QuickShop.instance().getDataFolder(), "itemi18n.yml");
        File ench = new File(QuickShop.instance().getDataFolder(), "enchi18n.yml");
        File potion = new File(QuickShop.instance().getDataFolder(), "potioni18n.yml");
        item.delete();
        ench.delete();
        potion.delete();
        QuickShop.instance().getLocaleManager().MINECRAFT_LOCALE.reload();
        QuickShop.instance().getLocaleManager().loadItemi18n();
        QuickShop.instance().getLocaleManager().loadEnchi18n();
        QuickShop.instance().getLocaleManager().loadPotioni18n();
        sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("complete", sender));
        break;
      case "config":
        File config = new File(QuickShop.instance().getDataFolder(), "config.yml");
        config.delete();
        QuickShop.instance().saveDefaultConfig();
        QuickShop.instance().reloadConfig();
        Bukkit.getPluginManager().disablePlugin(QuickShop.instance());
        Bukkit.getPluginManager().enablePlugin(QuickShop.instance());
        sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("complete", sender));
        break;
      case "messages":
        File msgs = new File(QuickShop.instance().getDataFolder(), "messages.json");
        msgs.delete();
        QuickShop.instance().getLocaleManager().loadCfgMessages();
        sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("complete", sender));
        break;
    }
  }
}
