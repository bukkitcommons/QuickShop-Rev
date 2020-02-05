/*
 * This file is a part of project QuickShop, the name is SubCommand_Info.java Copyright (C)
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.shop.ContainerShop;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopChunk;
import org.maxgamer.quickshop.shop.api.ShopType;

public class SubCommand_Info implements CommandProcesser {

  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    return new ArrayList<>();
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    int buying, selling, doubles, chunks, worlds, doubleschests;
    buying = selling = doubles = chunks = worlds = doubleschests = 0;
    int nostock = 0;

    for (Map<ShopChunk, Map<Location, Shop>> inWorld : ShopManager.instance().getShops()
        .values()) {
      worlds++;

      for (Map<Location, Shop> inChunk : inWorld.values()) {
        chunks++;
        // noinspection unchecked
        for (Shop shop : (ArrayList<Shop>) new ArrayList<>(inChunk.values()).clone()) {
          if (shop.is(ShopType.BUYING)) {
            buying++;
          } else {
            selling++;
          }

          if (shop instanceof ContainerShop && ((ContainerShop) shop).isDoubleShop()) {
            doubles++;
          } else if (shop.is(ShopType.SELLING) && shop.getRemainingStock() == 0) {
            nostock++;
          }

          if (shop instanceof ContainerShop && ((ContainerShop) shop).isDoubleChestShop()) {
            doubleschests++;
          }
        }
      }
    }

    sender.sendMessage(ChatColor.RED + "QuickShop Statistics...");
    sender.sendMessage(ChatColor.GREEN + "Server UniqueID: " + QuickShop.instance().getServerUniqueID());
    sender.sendMessage(ChatColor.GREEN + "" + (buying + selling) + " shops in " + chunks
        + " chunks spread over " + worlds + " worlds.");
    sender.sendMessage(ChatColor.GREEN + "" + doubles + " double shops. (" + doubleschests
        + " shops create on double chest.)");
    sender.sendMessage(ChatColor.GREEN + "" + nostock
        + " nostock selling shops (excluding doubles) which will be removed by /qs clean.");
    sender.sendMessage(ChatColor.GREEN + "QuickShop " + QuickShop.getVersion());
  }
}
