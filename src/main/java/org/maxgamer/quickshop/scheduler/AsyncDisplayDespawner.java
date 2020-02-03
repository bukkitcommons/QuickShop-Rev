package org.maxgamer.quickshop.scheduler;

import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.utils.Util;

@AllArgsConstructor
public class AsyncDisplayDespawner implements Runnable {
  private QuickShop plugin;

  @Override
  public void run() {
    int range = BaseConfig.despawnerRange;

    plugin.getShopManager().getLoadedShops().parallelStream()
        .filter(shop -> shop.getDisplay() != null).forEach(shop -> {
          // Check the range has player?
          boolean anyPlayerInRegion = Bukkit.getOnlinePlayers().stream()
              .filter(player -> player.getWorld() == shop.getLocation().getWorld())
              .anyMatch(player -> player.getLocation().distance(shop.getLocation()) < range);

          if (anyPlayerInRegion) {
            if (!shop.getDisplay().isSpawned()) {
              Util.debugLog("Respawning the shop " + shop
                  + " the display, cause it was despawned and a player close it");
              Bukkit.getScheduler().runTask(plugin, shop::checkDisplay);
            }
          } else if (shop.getDisplay().isSpawned()) {
            removeDisplayItemDelayed(shop);
          }
        });
  }

  public boolean removeDisplayItemDelayed(Shop shop) {
    if (shop.getDisplay().isPendingRemoval()) {
      // Actually remove the pending display
      Util.debugLog("Removing the shop " + shop + " the display, cause nobody can see it");
      Bukkit.getScheduler().runTask(plugin, () -> shop.getDisplay().remove());
      return true;
    } else {
      // Delayed to next calling
      Util.debugLog("Pending to remove the shop " + shop + " the display, cause nobody can see it");
      shop.getDisplay().pendingRemoval();
      return false;
    }
  }
}
