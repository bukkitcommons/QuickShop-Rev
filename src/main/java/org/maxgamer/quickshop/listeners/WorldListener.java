package org.maxgamer.quickshop.listeners;

import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.impl.ShopLoader;

@Deprecated // merge with loader
public class WorldListener implements Listener {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldLoad(WorldLoadEvent event) {
    ShopLoader.loadShops(event.getWorld());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    for (Chunk chunk : event.getWorld().getLoadedChunks()) {
      final Map<Location, Shop> inChunk = QuickShop.instance().getShopManager().getShops(chunk);

      if (inChunk != null && !inChunk.isEmpty())
        for (Shop shop : inChunk.values())
          shop.onUnload(); // FIXME performance
    }
  }
}
