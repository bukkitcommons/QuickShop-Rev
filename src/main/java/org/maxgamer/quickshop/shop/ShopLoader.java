package org.maxgamer.quickshop.shop;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopModerator;
import org.maxgamer.quickshop.shop.api.ShopType;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.viewer.ViewAction;

/** A class allow plugin load shops fast and simply. */
public class ShopLoader implements Listener {
  /*
   * Singleton
   */
  private static class LazySingleton {
    private static final ShopLoader INSTANCE = new ShopLoader();
  }
  
  private ShopLoader() {
    Bukkit.getPluginManager().registerEvents(this, QuickShop.instance());
  }
  
  public static ShopLoader instance() {
    return LazySingleton.INSTANCE;
  }
  
  @Getter
  private final Map<String, Map<Long, Map<Long, Shop>>> allShops = Maps.newHashMap();
  
  /**
   * Returns a new shop iterator object, allowing iteration over shops easily, instead of sorting
   * through a 3D hashmap.
   *
   * @return a new shop iterator object.
   */
  public Iterator<Shop> getShopIterator() {
    return new ShopIterator();
  }

  /**
   * Returns all shops in the whole database, include unloaded.
   *
   * <p>
   * Make sure you have caching this, because this need a while to get all shops
   *
   * @return All shop in the database
   */
  public Collection<Shop> getAllShop() {
    // noinspection unchecked
    Map<String, Map<Long, Map<Long, Shop>>> worldsMap = Maps.newHashMap(allShops);
    Collection<Shop> shops = new ArrayList<>();
    for (Map<Long, Map<Long, Shop>> shopMapData : worldsMap.values()) {
      for (Map<Long, Shop> shopData : shopMapData.values()) {
        shops.addAll(shopData.values());
      }
    }
    return shops;
  }
  
  public class ShopIterator implements Iterator<Shop> {
    private Iterator<Map<Long, Shop>> chunks;
    private Iterator<Shop> shops;
    private Iterator<Map<Long, Map<Long, Shop>>> worlds;

    public ShopIterator() {
      // noinspection unchecked
      Map<String, Map<Long, Map<Long, Shop>>> worldsMap = Maps.newHashMap(allShops);
      
      worlds = worldsMap.values().iterator();
    }

    /** Returns true if there is still more shops to iterate over. */
    @Override
    public boolean hasNext() {
      if (shops == null || !shops.hasNext()) {
        if (chunks == null || !chunks.hasNext()) {
          if (!worlds.hasNext()) {
            return false;
          } else {
            chunks = worlds.next().values().iterator();
            return hasNext();
          }
        } else {
          shops = chunks.next().values().iterator();
          return hasNext();
        }
      }
      return true;
    }

    /** Fetches the next shop. Throws NoSuchElementException if there are no more shops. */
    @Override
    public @NotNull Shop next() {
      if (shops == null || !shops.hasNext()) {
        if (chunks == null || !chunks.hasNext()) {
          if (!worlds.hasNext()) {
            throw new NoSuchElementException("No more shops to iterate over!");
          }
          chunks = worlds.next().values().iterator();
        }
        shops = chunks.next().values().iterator();
      }
      if (!shops.hasNext()) {
        return this.next(); // Skip to the next one (Empty iterator?)
      }
      return shops.next();
    }
  }
  
  /**
   * Returns a hashmap of Shops
   *
   * @param c The chunk to search. Referencing doesn't matter, only coordinates and world are used.
   * @return Shops
   */
  public @Nullable Map<Long, Shop> getShops(@NotNull Chunk c) {
    return getShops(c.getWorld().getName(), c.getX(), c.getZ());
  }

  public @Nullable Map<Long, Shop> getShops(@NotNull String world, int chunkX, int chunkZ) {
    @Nullable Map<Long, Map<Long, Shop>> inWorld = this.getShops(world);
    if (inWorld == null) {
      return null;
    }
    return inWorld.get(Util.chunkKey(chunkX, chunkZ));
  }

  /**
   * Returns a hashmap of Chunk - Shop
   *
   * @param world The name of the world (case sensitive) to get the list of shops from
   * @return a hashmap of Chunk - Shop
   */
  public @Nullable Map<Long, Map<Long, Shop>> getShops(@NotNull String world) {
    return this.allShops.get(world);
  }
  
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    String world = event.getWorld().getName();
    
    ShopManager.instance()
               .getLoadedShops()
               .stream()
               .filter(shop -> shop.getLocation().getWorld().getName().equals(world))
               .forEach(shop -> ShopManager.instance().unload(shop));
  }
  
  @EventHandler(priority = EventPriority.MONITOR)
  public void onWorldLoad(WorldLoadEvent event) {
    loadShopsForWorld(event.getWorld());
  }
  
  @EventHandler(priority = EventPriority.MONITOR)
  public void onChunkLoad(ChunkLoadEvent event) {
    if (event.isNewChunk())
      return;

    @Nullable Map<Long, Shop> inChunk = getShops(event.getChunk());
    
    if (inChunk != null && !inChunk.isEmpty())
      Bukkit.getScheduler().runTask(QuickShop.instance(), () -> {
        inChunk.values().forEach(Shop::onLoad);
      });
  }
  
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onChunkUnload(ChunkUnloadEvent e) {

    @Nullable Map<Long, Shop> inChunk = getShops(e.getChunk());

    if (inChunk != null && !inChunk.isEmpty())
      Bukkit.getScheduler().runTask(QuickShop.instance(), () -> {
        inChunk.values().forEach(Shop::onUnload);
      });
  }

  /**
   * Load all shops
   *
   * @param worldName The world name
   */
  public void loadShopsForWorld(@NotNull World world) {
    long onLoad = System.currentTimeMillis();
    
    try {
      ShopLogger.instance().info("Loading shops from the database..");
      long onFetch = System.currentTimeMillis();
      ResultSet rs = QuickShop.instance().getDatabaseHelper().selectAllShops();
      long durFetch = System.currentTimeMillis() - onFetch;
      long fetchSize = rs.getFetchSize();
      ShopLogger.instance().info("Fetched" + fetchSize + "shops from database by " + durFetch + "ms");
      
      long loadedShops = 0;
      long durTotalShopsNano = 0;
      
      while (rs.next()) {
        long onPerShop = System.nanoTime();
        ShopDatabaseInfo data = new ShopDatabaseInfo(rs);
        
        if (!data.world().equals(world.getName())) {
          durTotalShopsNano = System.nanoTime() - onPerShop;
          continue;
        }
        
        if (!canLoad(data)) {
          Util.debugLog("Somethings gone wrong, skipping the loading...");
          durTotalShopsNano = System.nanoTime() - onPerShop;
          continue;
        }
        
        Shop shop = new ContainerShop(
            new Location(Bukkit.getWorld(data.world()), data.x(), data.y(), data.z()),
            data.price(), data.item(),
            data.moderators(), data.unlimited(), data.type());
        
        if (Util.isChunkLoaded(shop.getLocation())) {
          // Load to World
          if (Util.canBeShop(shop.getLocation().getBlock())) {
            loadedShops++;
            ShopManager.instance().load(shop);
            shop.onLoad();
          }
        }
        
        durTotalShopsNano = System.nanoTime() - onPerShop;
      }
      
      long durLoad = System.currentTimeMillis() - onLoad;
      long averagePerShop = durTotalShopsNano / loadedShops;
      
      ShopLogger.instance().info(
          "Successfully loaded " + loadedShops + " of " + fetchSize + " shops in" + world.getName() + "! " +
          "(Total: " + durLoad + "ms, Fetch: " + durFetch + "ms," +
          " Load: " + (durTotalShopsNano / 1000000) + "ms, Avg Per: " + averagePerShop + "ns)");
      
    } catch (Throwable t) {
      exceptionHandler(t, null);
    }
  }

  private static boolean canLoad(@NotNull ShopDatabaseInfo info) {
    if (info.item() == null)
      return false;
    
    if (info.item().getType() == Material.AIR)
      return false;
    
    if (info.type() == null)
      return false;
    
    if (info.moderators() == null)
      return false;
    
    if (info.world() == null)
      return false;
    
    if (info.moderators() == null)
      return false;
    
    return true;
  }

  private static @Nullable ItemStack deserializeItem(@NotNull String itemConfig) {
    try {
      return Util.deserialize(itemConfig);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
      ShopLogger.instance().warning(
          "Failed load shop data, because target config can't deserialize the ItemStack.");
      Util.debugLog("Failed to load data to the ItemStack: " + itemConfig);
      return null;
    }
  }

  private static @Nullable ShopModerator deserializeModerator(@NotNull String moderatorJson) {
    ShopModerator shopModerator;
    if (Util.isUUID(moderatorJson)) {
      Util.debugLog("Updating old shop data... for " + moderatorJson);
      shopModerator = new ShopModerator(UUID.fromString(moderatorJson)); // New one
    } else {
      try {
        shopModerator = ShopModerator.deserialize(moderatorJson);
      } catch (JsonSyntaxException ex) {
        Util.debugLog("Updating old shop data... for " + moderatorJson);
        moderatorJson = Bukkit.getOfflinePlayer(moderatorJson).getUniqueId().toString();
        shopModerator = new ShopModerator(UUID.fromString(moderatorJson)); // New one
      }
    }
    return shopModerator;
  }

  @Data
  @Accessors(fluent = true)
  public static class ShopDatabaseInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @NotNull private ItemStack item;
    @NotNull private ShopModerator moderators;
    @NotNull private String world;
    @NotNull private ShopType type;
    
    private double price;
    private boolean unlimited;
    private int x;
    private int y;
    private int z;
    
    public ShopDatabaseInfo(@NotNull ResultSet rs) {
      try {
        this.x = rs.getInt("x");
        this.y = rs.getInt("y");
        this.z = rs.getInt("z");
        this.price = rs.getDouble("price");
        this.unlimited = rs.getBoolean("unlimited");
        this.type = ShopType.fromID(rs.getInt("type"));
        this.world = rs.getString("world");
        this.item = deserializeItem(rs.getString("itemConfig"));
        this.moderators = deserializeModerator(rs.getString("owner"));
        
      } catch (Throwable t) {
        exceptionHandler(t, rs);
      }
    }
  }
  
  private static Object getSafely(ResultSet set, String pos, List<Throwable> handler) {
    try {
      Object value = set.getObject("world");
      return value == null ? "~NULL~" : value;
    } catch (Throwable t) {
      handler.add(t);
      return "(Error) Failed to obtain, nested error id @ " + (handler.size() - 1);
    }
  }
  
  private static void exceptionHandler(@NotNull Throwable throwable, @Nullable ResultSet set) {
    Logger logger = ShopLogger.instance();
    
    logger.warning("########## FAILED TO LOAD SHOP FROM DB ##########");
    logger.warning("  >> Error Info:");
    String message = throwable.getMessage();
    message = message == null ? "~NULL~" : message;
    logger.warning("  > " + message);
    
    logger.warning("  >> Error Trace");
    throwable.printStackTrace();
    
    if (set != null) {
      logger.warning("  >> Shop Info");
      Object temp; List<Throwable> nested = Lists.newArrayList();
      
      try {
        temp = String.valueOf(temp = getSafely(set, "world", nested));
      } catch (Throwable t) {
        temp = "(Error) World name is corrupted, nested error id @ " + (nested.size() - 1);
      }
      logger.warning("  >  World: " + temp);
      try {
        temp = Integer.valueOf(String.valueOf((temp = getSafely(set, "x", nested))));
      } catch (Throwable t) {
        temp = "(Error) X pos is corrupted, nested error id @ " + (nested.size() - 1);
      }
      logger.warning("  >  X: " + temp);
      try {
        temp = Integer.valueOf(String.valueOf((temp = getSafely(set, "y", nested))));
      } catch (Throwable t) {
        temp = "(Error) Y pos is corrupted, nested error id @ " + (nested.size() - 1);
      }
      logger.warning("  >  Y: " + temp);
      try {
        temp = Integer.valueOf(String.valueOf((temp = getSafely(set, "z", nested))));
      } catch (Throwable t) {
        temp = "(Error) Z pos is corrupted, nested error id @ " + (nested.size() - 1);
      }
      logger.warning("  >  Z: " + temp);
      try {
        temp = Double.valueOf(String.valueOf((temp = getSafely(set, "price", nested))));
      } catch (Throwable t) {
        temp = "(Error) Pirce data is corrupted, nested error id @ " + (nested.size() - 1);
      }
      logger.warning("  >  Price: " + temp);
      try {
        temp = Boolean.valueOf(String.valueOf((temp = getSafely(set, "unlimited", nested))));
      } catch (Throwable t) {
        temp = "(Error) Unlimited type is corrupted, nested error id @ " + (nested.size() - 1);
      }
      logger.warning("  >  Unlimited: " + temp);
      try {
        temp = Integer.valueOf(String.valueOf((temp = getSafely(set, "type", nested))));
      } catch (Throwable t) {
        temp = "(Error) Shop type is corrupted, nested error id @ " + (nested.size() - 1);
      }
      logger.warning("  >  Shop Type: " + temp);
      try {
        temp = String.valueOf((temp = getSafely(set, "itemConfig", nested)));
      } catch (Throwable t) {
        temp = "(Error) Item data is corrupted, nested error id @ " + (nested.size() - 1);
      }
      logger.warning("  >  Item: " + temp);
      try {
        temp = String.valueOf((temp = getSafely(set, "owner", nested)));
      } catch (Throwable t) {
        temp = "(Error) Owner data is corrupted, nested error id @ " + (nested.size() - 1);
      }
      logger.warning("  >> Owner: " + temp);
      
      logger.warning("  >> Nested Errors");
      for (int i = 0; i < nested.size(); i++) {
        logger.warning("  >  Id " + i);
        nested.get(i).printStackTrace();
        logger.warning("");
      }
    }
    
    logger.warning("  >> Database Info");
    try {
      logger.warning("  >  Connected: " + QuickShop.instance().getDatabase().getConnection().isClosed());
    } catch (SQLException | NullPointerException e) {
      logger.warning("  >  Connected: " + "Failed to load status.");
    }
    try {
      logger.warning("  >  Readonly: " + QuickShop.instance().getDatabase().getConnection().isReadOnly());
    } catch (SQLException | NullPointerException e) {
      logger.warning("  >  Readonly: " + "Failed to load status.");
    }
    try {
      logger.warning("  >  ClientInfo: " + QuickShop.instance().getDatabase().getConnection().getClientInfo());
    } catch (SQLException | NullPointerException e) {
      logger.warning("  >  ClientInfo: " + "Failed to load status.");
    }
    
    logger.warning("#######################################");
  }
}
