/*
 * This file is a part of project QuickShop, the name is SubCommand_SuperCreate.java Copyright (C)
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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.shop.ShopActionManager;
import org.maxgamer.quickshop.shop.QuickShopManager;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.data.ShopAction;
import cc.bukkit.shop.data.ShopCreator;
import cc.bukkit.shop.data.ShopLocation;
import cc.bukkit.shop.data.ShopSnapshot;

public class SubCommand_SuperCreate implements CommandProcesser {

  private final QuickShop plugin = QuickShop.instance;

  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    final ArrayList<String> list = new ArrayList<>();

    list.add(MsgUtil.getMessage("tabcomplete.amount", sender));

    return list;
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("This command can't be run by console");
      return;
    }

    final Player p = (Player) sender;
    final ItemStack item = p.getInventory().getItemInMainHand();

    if (item.getType() == Material.AIR) {
      sender.sendMessage(MsgUtil.getMessage("no-anythings-in-your-hand", sender));
      return;
    }

    final BlockIterator bIt = new BlockIterator((LivingEntity) sender, 10);

    while (bIt.hasNext()) {
      final Block b = bIt.next();

      if (!Util.canBeShop(b)) {
        Util.debug("Block cannot be shop.");
        continue;
      }

      if (!QuickShopManager.canBuildShop(p, b)) {
        // As of the new checking system, most plugins will tell the
        // player why they can't create a shop there.
        // So telling them a message would cause spam etc.
        Util.debug("Util report you can't build shop there.");
        return;
      }

      if (Util.getSecondHalf(b).isPresent()
          && !PermissionManager.instance().has(sender, "quickshop.create.double")) {
        p.sendMessage(MsgUtil.getMessage("no-double-chests", sender));
        return;
      }

      if (Util.isBlacklisted(item) && !PermissionManager.instance().has(p,
          "quickshop.bypass." + item.getType().name())) {
        p.sendMessage(MsgUtil.getMessage("blacklisted-item", sender));
        return;
      }

      if (cmdArg.length >= 1) {
        ShopActionManager.instance().handleChat(p, cmdArg[0], true);
        return;
      }
      // Send creation menu.
      final ShopCreator info = ShopCreator.create(ShopLocation.of(b.getLocation()), b.getRelative(p.getFacing().getOppositeFace()), item);

      ShopActionManager.instance().getActions().put(p.getUniqueId(), info);
      p.sendMessage(
          MsgUtil.getMessage("how-much-to-trade-for", sender, Util.getItemStackName(item)));
      return;
    }
    sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
  }
}
