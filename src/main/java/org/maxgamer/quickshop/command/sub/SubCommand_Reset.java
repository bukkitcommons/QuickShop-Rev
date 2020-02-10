/*
 * This file is a part of project QuickShop, the name is SubCommand_Find.java Copyright (C)
 * Ghost_chu <https://github.com/Ghost-chu> Copyright (C) Bukkit Commons Studio and contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.maxgamer.quickshop.command.sub;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.command.CommandProcesser;

public class SubCommand_Reset implements CommandProcesser {
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
      sender.sendMessage(MsgUtil.getMessage("command.no-type-given", sender));
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
        MsgUtil.MINECRAFT_LOCALE.reload();
        MsgUtil.loadItemi18n();
        MsgUtil.loadEnchi18n();
        MsgUtil.loadPotioni18n();
        sender.sendMessage(MsgUtil.getMessage("complete", sender));
        break;
      case "config":
        File config = new File(QuickShop.instance().getDataFolder(), "config.yml");
        config.delete();
        QuickShop.instance().saveDefaultConfig();
        QuickShop.instance().reloadConfig();
        Bukkit.getPluginManager().disablePlugin(QuickShop.instance());
        Bukkit.getPluginManager().enablePlugin(QuickShop.instance());
        sender.sendMessage(MsgUtil.getMessage("complete", sender));
        break;
      case "messages":
        File msgs = new File(QuickShop.instance().getDataFolder(), "messages.json");
        msgs.delete();
        MsgUtil.loadCfgMessages();
        sender.sendMessage(MsgUtil.getMessage("complete", sender));
        break;
    }
  }
}
