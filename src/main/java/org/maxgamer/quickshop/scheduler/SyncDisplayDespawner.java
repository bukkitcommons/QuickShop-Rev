package org.maxgamer.quickshop.scheduler;

import org.bukkit.Bukkit;
import org.maxgamer.quickshop.configuration.BaseConfig;
import cc.bukkit.shop.BasicShop;
import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.Shop;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SyncDisplayDespawner implements Runnable {
  @Override
  public void run() {
    int range = BaseConfig.despawnerRange;

    Shop.getManager().viewLoadedShops(shops ->
      shops.parallelStream()
        .filter(shop -> ((ChestShop) shop).display() != null).forEach((BasicShop shop) -> {
          // Check the range has player?
          boolean anyPlayerInRegion = Bukkit.getOnlinePlayers().stream()
              .filter(player -> player.getWorld().getName().equals(shop.location().worldName()))
              .anyMatch(player -> player.getLocation().distance(shop.location().bukkit()) < range);

          /*
          if (anyPlayerInRegion) {
            if (!((ChestShop) shop).display().isSpawned()) {
              Util.debug("Respawning the shop " + shop
                  + " the display, cause it was despawned and a player close it");
              //Bukkit.getScheduler().runTask(QuickShop.instance(), shop::checkDisplay);
            }
          } else if ((ChestShop) shop).isSpawned()) {
            removeDisplayItemDelayed(shop);
          }
          */
        })
      );
  }

  public boolean removeDisplayItemDelayed(ChestShop shop) {
    /*
    if (shop.display().isPendingRemoval()) {
      // Actually remove the pending display
      Util.debug("Removing the shop " + shop + " the display, cause nobody can see it");
      shop.display().remove();
      return true;
    } else {
      // Delayed to next calling
      Util.debug("Pending to remove the shop " + shop + " the display, cause nobody can see it");
      shop.display().pendingRemoval();
      return false;
    }
    */
    return false;
  }
}
