package org.maxgamer.quickshop.scheduler;

import org.bukkit.Bukkit;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.Shop;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SyncDisplayDespawner implements Runnable {
  @Override
  public void run() {
    int range = BaseConfig.despawnerRange;

    Shop.getManager().viewLoadedShops(shops ->
      shops.parallelStream()
        .filter(shop -> shop.getDisplay() != null).forEach(shop -> {
          // Check the range has player?
          boolean anyPlayerInRegion = Bukkit.getOnlinePlayers().stream()
              .filter(player -> player.getWorld().getName().equals(shop.getLocation().worldName()))
              .anyMatch(player -> player.getLocation().distance(shop.getLocation().bukkit()) < range);

          if (anyPlayerInRegion) {
            if (!shop.getDisplay().isSpawned()) {
              Util.debug("Respawning the shop " + shop
                  + " the display, cause it was despawned and a player close it");
              Bukkit.getScheduler().runTask(QuickShop.instance(), shop::checkDisplay);
            }
          } else if (shop.getDisplay().isSpawned()) {
            removeDisplayItemDelayed(shop);
          }
        })
      );
  }

  public boolean removeDisplayItemDelayed(ContainerShop shop) {
    if (shop.getDisplay().isPendingRemoval()) {
      // Actually remove the pending display
      Util.debug("Removing the shop " + shop + " the display, cause nobody can see it");
      shop.getDisplay().remove();
      return true;
    } else {
      // Delayed to next calling
      Util.debug("Pending to remove the shop " + shop + " the display, cause nobody can see it");
      shop.getDisplay().pendingRemoval();
      return false;
    }
  }
}
