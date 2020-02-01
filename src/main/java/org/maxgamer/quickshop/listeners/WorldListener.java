package org.maxgamer.quickshop.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.ShopChunk;

public class WorldListener implements Listener {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldLoad(WorldLoadEvent event) {
    final World world = event.getWorld();

    QuickShop.instance().getShopLoader().loadShops(world.getName());
    // New world data
    final HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = new HashMap<>(1);
    // Old world data
    final HashMap<ShopChunk, HashMap<Location, Shop>> oldInWorld =
        QuickShop.instance().getShopManager().getShops(world.getName());
    // Nothing in the old world, therefore we don't care. No locations to
    // update.
    if (oldInWorld == null) {
      return;
    }

    for (Entry<ShopChunk, HashMap<Location, Shop>> oldInChunk : oldInWorld.entrySet()) {
      final HashMap<Location, Shop> inChunk = new HashMap<>(1);
      // Put the new chunk were the old chunk was
      inWorld.put(oldInChunk.getKey(), inChunk);

      for (Entry<Location, Shop> entry : oldInChunk.getValue().entrySet()) {
        final Shop shop = entry.getValue();

        shop.getLocation().setWorld(world);
        inChunk.put(shop.getLocation(), shop);
      }
    }
    // Done - Now we can store the new world dataz!

    QuickShop.instance().getShopManager().getShops().put(world.getName(), inWorld);
    // This is a workaround, because I don't get parsed chunk events when a
    // world first loads....
    // So manually tell all of these shops they're loaded.
    for (Chunk chunk : world.getLoadedChunks()) {
      final HashMap<Location, Shop> inChunk = QuickShop.instance().getShopManager().getShops(chunk);

      if (inChunk == null || inChunk.isEmpty()) {
        continue;
      }

      for (Shop shop : inChunk.values()) {
        shop.onLoad();
      }
    }
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
