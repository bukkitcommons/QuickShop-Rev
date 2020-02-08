package org.maxgamer.quickshop.shop;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.event.ShopDeleteEvent;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopModerator;
import org.maxgamer.quickshop.shop.api.ShopType;
import org.maxgamer.quickshop.shop.api.data.ShopLocation;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.ShopLogger;

/** A class allow plugin load shops fast and simply. */
public class ShopLoader implements Listener {
  /*
   * Singleton
   */
  private static class LazySingleton {
    private static final ShopLoader INSTANCE = new ShopLoader();
  }
  
  private ShopLoader() {
    ShopLogger.instance().info("Loading shops..");
    Bukkit.getPluginManager().registerEvents(this, QuickShop.instance());
    Bukkit.getScheduler().runTask(QuickShop.instance(), this::loadShopsForAllWorlds);
  }
  
  public static ShopLoader instance() {
    return LazySingleton.INSTANCE;
  }
  
  /**
   * An memory reference of the database to hold all exist shops
   */
  @Getter
  private final Map<String, Map<Long, Map<Long, Shop>>> shopsMap = Maps.newConcurrentMap();

  /**
   * Returns all shops in the whole database, include unloaded.
   *
   * <p>
   * Make sure you have caching this, because this need a while to get all shops
   *
   * @return All shop in the database
   */
  public List<Shop> getAllShops() {
    List<Shop> shops = Lists.newArrayList();
    
    for (Map<Long, Map<Long, Shop>> worldShops : shopsMap.values()) {
      for (Map<Long, Shop> shopData : worldShops.values()) {
        shops.addAll(shopData.values());
      }
    }
    
    return shops;
  }
  
  public void forEachShops(Consumer<Shop> consumer) {
    for (Map<Long, Map<Long, Shop>> worldShops : shopsMap.values()) {
      for (Map<Long, Shop> chunkShops : worldShops.values()) {
        chunkShops.values().forEach(shop -> consumer.accept(shop));
      }
    }
  }
  
  /**
   * Returns a hashmap of Shops
   *
   * @param c The chunk to search. Referencing doesn't matter, only coordinates and world are used.
   * @return Shops
   */
  public @Nullable Map<Long, Shop> getShopsInChunk(@NotNull Chunk c) {
    return getShopsInChunk(c.getWorld().getName(), c.getX(), c.getZ());
  }

  public @Nullable Map<Long, Shop> getShopsInChunk(@NotNull String world, int chunkX, int chunkZ) {
    @Nullable Map<Long, Map<Long, Shop>> inWorld = this.getShopsInWorld(world);
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
  public @Nullable Map<Long, Map<Long, Shop>> getShopsInWorld(@NotNull String world) {
    return this.shopsMap.get(world);
  }
  
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    String world = event.getWorld().getName();
    
    ShopManager.instance()
               .getLoadedShops()
               .getOrDefault(world, Collections.emptyMap())
               .values()
               .forEach(blockMap -> blockMap.values().forEach(shop -> ShopManager.instance().unload(shop)));
    // Note: we do not actually remove the shop from memory,
    // to ensures it is able to get right data of existence when needed,
    // such as shop cleaner.
    
    event.getWorld().getPlayers().forEach(player -> {
      ShopActionManager.instance().getActions().remove(player.getUniqueId());
    });
  }
  
  public void delete(@NotNull Shop shop) {
    ShopDeleteEvent shopDeleteEvent = new ShopDeleteEvent(shop, false);
    if (Util.fireCancellableEvent(shopDeleteEvent)) {
      Util.debug("Shop deletion was canceled because a plugin canceled it.");
      return;
    }
    
    @Nullable Map<Long, Shop> inChunk = getShopsInChunk(shop.getLocation().chunk());
    if (inChunk != null && !inChunk.isEmpty())
      inChunk.remove(shop.getLocation().blockKey());
    
    ShopManager.instance().unload(shop);
    
    // Delete the display item
    if (shop.getDisplay() != null) {
      shop.getDisplay().remove();
    }
    
    // Delete the signs around it
    for (Sign s : shop.getShopSigns())
      s.getBlock().setType(Material.AIR);
    
    // Delete it from the database
    int x = shop.getLocation().x();
    int y = shop.getLocation().y();
    int z = shop.getLocation().z();
    String world = shop.getLocation().worldName();
    
    try {
      QuickShop.instance().getDatabaseHelper().deleteShop(x, y, z, world);
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      if (BaseConfig.refundable)
        QuickShop.instance().getEconomy().deposit(shop.getOwner(), BaseConfig.refundCost);
    }
  }
  
  @EventHandler(priority = EventPriority.MONITOR)
  public void onChunkLoad(ChunkLoadEvent event) {
    if (event.isNewChunk())
      return;

    @Nullable Map<Long, Shop> inChunk = getShopsInChunk(event.getChunk());
    
    if (inChunk != null && !inChunk.isEmpty())
      Bukkit.getScheduler().runTask(QuickShop.instance(), () -> {
        inChunk.values().forEach(shop -> ShopManager.instance().load(shop));
      });
  }
  
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onChunkUnload(ChunkUnloadEvent e) {

    @Nullable Map<Long, Shop> inChunk = getShopsInChunk(e.getChunk());

    if (inChunk != null && !inChunk.isEmpty())
      Bukkit.getScheduler().runTask(QuickShop.instance(), () -> {
        inChunk.values().forEach(shop -> ShopManager.instance().unload(shop));
      });
  }
  
  @EventHandler
  public void onWorldLoad(WorldLoadEvent event) {
    Bukkit.getScheduler().runTask(QuickShop.instance(), () -> {
      try {
        loadShopsFor(fetchShops(), event.getWorld());
      } catch (Throwable t) {
        exceptionHandler(t, null);
      }
    });
  }
  
  public void loadShopsForAllWorlds() {
    long onLoad = System.currentTimeMillis();
    
    try {
      ResultSet set = fetchShops();
      for (World world : Bukkit.getWorlds())
        loadShopsFor(set, world);
      
      long durLoad = System.currentTimeMillis() - onLoad;
      ShopLogger.instance().info("Done! Loaded all shops in " + durLoad + " ms");
    } catch (Throwable t) {
      exceptionHandler(t, null);
    }
  }
  
  public ResultSet fetchShops() throws SQLException {
    ShopLogger.instance().info("Fetching shops from database..");
    long onFetch = System.currentTimeMillis();
    ResultSet set = QuickShop.instance().getDatabaseHelper().selectAllShops();
    long durFetch = System.currentTimeMillis() - onFetch;
    ShopLogger.instance().info("Fetched all shops by " + durFetch + " ms");
    return set;
  }
  
  public void loadShopsFor(@NotNull ResultSet set, @NotNull World world) throws SQLException {
    Map<Long, Map<Long, Shop>> inWorld = shopsMap.computeIfAbsent(world.getName(), s -> new HashMap<>(3));
    
    long loadedShops = 0;
    long durTotalShopsNano = 0;
    
    while (set.next()) {
      long onPerShop = System.nanoTime();
      ShopDatabaseInfo data = new ShopDatabaseInfo(set);
      
      if (!data.world().equals(world.getName())) {
        durTotalShopsNano = System.nanoTime() - onPerShop;
        continue;
      }
      
      if (!canLoad(data)) {
        Util.debug("Somethings gone wrong, skipping the loading...");
        durTotalShopsNano = System.nanoTime() - onPerShop;
        continue;
      }
      
      Shop shop = new ContainerShop(
          ShopLocation.from(world, data.x(), data.y(), data.z()),
          data.price(), data.item(),
          data.moderators(), data.unlimited(), data.type());
      
      Map<Long, Shop> inChunk =
          inWorld.computeIfAbsent(Util.chunkKey(data.x() >> 4, data.z() >> 4), s -> Maps.newHashMap());
      inChunk.put(Util.blockKey(data.x(), data.y(), data.z()), shop);
      
      if (Util.isChunkLoaded(shop.getLocation())) {
        // Load to World
        if (Util.canBeShop(shop.getLocation().block())) {
          loadedShops++;
          ShopManager.instance().load(shop);
        }
      }
      
      durTotalShopsNano = System.nanoTime() - onPerShop;
    }
    
    if (loadedShops > 0) {
      long averagePerShop = durTotalShopsNano / loadedShops;
      
      ShopLogger.instance().info(
          "Loaded " + loadedShops + " shops in " + world.getName() + " ! " +
          " Total: " + (durTotalShopsNano / 1000000) + "ms, Avg Per: " + averagePerShop + " ns)");
    } else {
      ShopLogger.instance().info("No shop could be loaded in " + world.getName() + " !");
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
      Util.debug("Failed to load data to the ItemStack: " + itemConfig);
      return null;
    }
  }

  @SuppressWarnings("deprecation")
  private static @Nullable ShopModerator deserializeModerator(@NotNull String moderatorJson) {
    ShopModerator shopModerator;
    if (Util.isUUID(moderatorJson)) {
      Util.debug("Updating old shop data... for " + moderatorJson);
      shopModerator = new ShopModerator(UUID.fromString(moderatorJson)); // New one
    } else {
      try {
        shopModerator = ShopModerator.deserialize(moderatorJson);
      } catch (JsonSyntaxException ex) {
        Util.debug("Updating old shop data... for " + moderatorJson);
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
