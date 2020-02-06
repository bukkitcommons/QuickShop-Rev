/*
 * This file is a part of project QuickShop, the name is SubCommand_Staff.java Copyright (C)
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

public class SubCommand_Staff implements CommandProcesser {
  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    final ArrayList<String> tabList = new ArrayList<>();

    Util.debugLog(Util.array2String(cmdArg));

    if (cmdArg.length < 2) {
      if (cmdArg.length == 1) {
        final String prefix = cmdArg[0].toLowerCase();

        if ("add".startsWith(prefix) || "add".equals(prefix)) {
          tabList.add("add");
        }

        if ("del".startsWith(prefix) || "del".equals(prefix)) {
          tabList.add("del");
        }

        if ("list".startsWith(prefix) || "list".equals(prefix)) {
          tabList.add("list");
        }

        if ("clear".startsWith(prefix) || "clear".equals(prefix)) {
          tabList.add("clear");
        }
      } else {
        tabList.add("add");
        tabList.add("del");
        tabList.add("list");
        tabList.add("clear");
      }

      return tabList;
    }

    return tabList;
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    Util.debugLog(Util.array2String(cmdArg));
    if (!(sender instanceof Player)) {
      sender.sendMessage("Only player can execute this command.");
      return;
    }

    final BlockIterator bIt = new BlockIterator((LivingEntity) sender, 10);

    if (!bIt.hasNext()) {
      sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
      return;
    }

    while (bIt.hasNext()) {
      final Block b = bIt.next();
      final ShopViewer shop = ShopManager.instance().getLoadedShopAt(b.getLocation());

      if (!shop.isPresent() || !shop.get().getModerator().isModerator(((Player) sender).getUniqueId())) {
        continue;
      }

      switch (cmdArg.length) {
        case 0:
          sender.sendMessage(MsgUtil.getMessage("command.wrong-args", sender));
          return;
        case 1:
          switch (cmdArg[0]) {
            case "add":
              sender.sendMessage(MsgUtil.getMessage("command.wrong-args", sender));
              return;
            case "del":
              sender.sendMessage(MsgUtil.getMessage("command.wrong-args", sender));
              return;
            case "clear":
              shop.get().clearStaffs();
              sender.sendMessage(MsgUtil.getMessage("shop-staff-cleared", sender));
              return;
            case "list":
              final Set<UUID> staffs = shop.get().getStaffs();

              if (staffs.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN
                    + MsgUtil.getMessage("tableformat.left_begin", sender) + "Empty");
                return;
              }

              for (UUID uuid : staffs) {
                sender.sendMessage(
                    ChatColor.GREEN + MsgUtil.getMessage("tableformat.left_begin", sender)
                        + Bukkit.getOfflinePlayer(uuid).getName());
              }

              return;
            default:
              sender.sendMessage(MsgUtil.getMessage("command.wrong-args", sender));
          }

          break;
        case 2:
          final OfflinePlayer offlinePlayer = QuickShop.instance().getServer().getOfflinePlayer(cmdArg[1]);
          String offlinePlayerName = offlinePlayer.getName();

          if (offlinePlayerName == null) {
            offlinePlayerName = "null";
          }

          switch (cmdArg[0]) {
            case "add":
              shop.get().addStaff(offlinePlayer.getUniqueId());
              sender.sendMessage(MsgUtil.getMessage("shop-staff-added", sender, offlinePlayerName));
              return;
            case "del":
              sender
                  .sendMessage(MsgUtil.getMessage("shop-staff-deleted", sender, offlinePlayerName));
              return;
            default:
              sender.sendMessage(MsgUtil.getMessage("command.wrong-args", sender));
          }

          break;
        default:
          Util.debugLog("No any args matched");
          break;
      }
    }
  }
}
