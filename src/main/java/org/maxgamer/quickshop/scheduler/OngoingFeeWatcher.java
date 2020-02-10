package org.maxgamer.quickshop.scheduler;

import java.sql.SQLException;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.shop.QuickShopLoader;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.data.ShopData;

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
    
    QuickShopLoader.instance().forEachShops(shop -> {
      if (!shop.unlimited() || !ignoreUnlimited) {
        UUID shopOwner = shop.moderators().getOwner();
        
        if (!allowLoan) {
          // Disallow loan
          if (plugin.getEconomy().getBalance(shopOwner) < cost) {
            this.removeShop(shop);
          }
        }
        boolean success = plugin.getEconomy().withdraw(shop.moderators().getOwner(), cost);
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
  public void removeShop(@NotNull ShopData shop) {
    Bukkit.getScheduler().runTask(plugin, () -> {
      try {
        QuickShopLoader.instance().delete(shop);
        
        if (!shop.unlimited() || !BaseConfig.ignoreUnlimitedMessages)
          MsgUtil.send(shop.moderators().getOwner(),
              MsgUtil.getMessagePlaceholder("shop-removed-cause-ongoing-fee",
                  Bukkit.getOfflinePlayer(shop.moderators().getOwner()),
                  "World:" + shop.world() + " X:" + shop.x() +
                  " Y:" + shop.y() + " Z:" + shop.z()));
        
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });
  }
}
