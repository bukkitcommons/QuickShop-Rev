/*
 * This file is a part of project QuickShop, the name is SubCommand_SetOwner.java Copyright (C)
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
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.utils.MsgUtil;

public class SubCommand_SetOwner implements CommandProcesser {

  private final QuickShop plugin = QuickShop.instance;

  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    return new ArrayList<>();
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(MsgUtil.getMessage("Only player can run this command", sender));
      return;
    }

    if (cmdArg.length < 1) {
      sender.sendMessage(MsgUtil.getMessage("command.no-owner-given", sender));
      return;
    }

    final BlockIterator bIt = new BlockIterator((Player) sender, 10);

    if (!bIt.hasNext()) {
      sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
      return;
    }

    while (bIt.hasNext()) {
      final Block b = bIt.next();
      final Optional<Shop> shop = plugin.getShopManager().getShop(b.getLocation());

      if (!shop.isPresent()) {
        continue;
      }

      @SuppressWarnings("deprecation")
      final OfflinePlayer p = plugin.getServer().getOfflinePlayer(cmdArg[0]);
      final String shopOwner = plugin.getServer().getOfflinePlayer(shop.get().getOwner()).getName();
      if (shopOwner == null) {
        sender.sendMessage(MsgUtil.getMessage("unknown-player", null));
        return;
      }
      shop.get().setOwner(p.getUniqueId());
      // shop.setSignText();
      shop.get().update();
      sender.sendMessage(MsgUtil.getMessage("command.new-owner", sender, shopOwner));
      return;
    }

    sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
  }
}
