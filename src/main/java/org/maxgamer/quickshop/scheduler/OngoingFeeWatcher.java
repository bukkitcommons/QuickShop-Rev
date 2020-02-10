/*
 * This file is a part of project QuickShop, the name is OngoingFeeWatcher.java Copyright (C)
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

package org.maxgamer.quickshop.scheduler;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.shop.ShopLoader;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;

/**
 * Check the shops after server booted up, make sure shop can correct self-deleted when container
 * lost.
 */
public class OngoingFeeWatcher extends BukkitRunnable {
  private QuickShop plugin;

  public OngoingFeeWatcher(@NotNull QuickShop plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
    Util.debug("Run task for ongoing fee...");
    if (plugin.getEconomy() == null) {
      Util.debug("Economy hadn't get ready.");
      return;
    }
    int cost = BaseConfig.ongoingFeeCostPerShop;
    boolean allowLoan = BaseConfig.allowLoan;
    boolean ignoreUnlimited = BaseConfig.ongoingFeeIgnoreUnlimited;
    
    ShopLoader.instance().forEachShops(shop -> {
      if (!shop.isUnlimited() || !ignoreUnlimited) {
        UUID shopOwner = shop.getOwner();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
          if (!allowLoan) {
            // Disallow loan
            if (plugin.getEconomy().getBalance(shopOwner) < cost) {
              this.removeShop(shop);
            }
          }
          boolean success = plugin.getEconomy().withdraw(shop.getOwner(), cost);
          if (!success) {
            this.removeShop(shop);
          } else {
            try {
              // noinspection ConstantConditions,deprecation
              plugin.getEconomy()
                  .deposit(Bukkit.getOfflinePlayer(BaseConfig.taxAccount).getUniqueId(), cost);
            } catch (Exception ignored) {
            }
          }
        }); // FIXME
      } else {
        Util.debug(
            "Shop was ignored for ongoing fee cause it is unlimited and ignoreUnlimited = true : "
                + shop);
      }
    });
  }

  /**
   * Remove shop and send alert to shop owner
   *
   * @param shop The shop was remove cause no enough ongoing fee
   */
  public void removeShop(@NotNull Shop shop) {
    Bukkit.getScheduler().runTask(plugin, () -> ShopLoader.instance().delete(shop));

    if (!shop.isUnlimited() || !BaseConfig.ignoreUnlimitedMessages)
      MsgUtil.send(shop.getOwner(),
          MsgUtil.getMessagePlaceholder("shop-removed-cause-ongoing-fee",
              Bukkit.getOfflinePlayer(shop.getOwner()),
              "World:" + shop.getLocation().worldName() + " X:"
                  + shop.getLocation().x() + " Y:" + shop.getLocation().y() + " Z:"
                  + shop.getLocation().z()));
  }
}
