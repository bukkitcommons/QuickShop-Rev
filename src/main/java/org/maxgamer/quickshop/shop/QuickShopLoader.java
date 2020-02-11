package org.maxgamer.quickshop.shop;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
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
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopLoader;
import cc.bukkit.shop.action.data.ShopData;
import cc.bukkit.shop.event.ShopDeleteEvent;
import cc.bukkit.shop.moderator.ShopModerator;
import cc.bukkit.shop.util.ShopLogger;
import cc.bukkit.shop.util.Utils;
import cc.bukkit.shop.viewer.ShopViewer;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

/** A class allow plugin load shops fast and simply. */
public class QuickShopLoader implements ShopLoader, Listener {
  /*
   * Singleton
   */
  private static class LazySingleton {
    private static final QuickShopLoader INSTANCE = new QuickShopLoader();
  }
  
  private QuickShopLoader() {
    Bukkit.getPluginManager().registerEvents(this, QuickShop.instance());
  }
  
  @Override
  public void loadShops() {
    ShopLogger.instance().info("Loading shops..");
    loadShopsForAllWorlds();
  }
  
  public static QuickShopLoader instance() {
    return LazySingleton.INSTANCE;
  }
  
  /**
   * An memory reference of the database to hold all exist shops
   */
  @Getter
  private final Map<String, Map<Long, Map<Long, ShopData>>> shopsMap = Maps.newConcurrentMap();

  /**
   * Returns all shops in the whole database, include unloaded.
   *
   * <p>
   * Make sure you have caching this, because this need a while to get all shops
   *
   * @return All shop in the database
   */
  @NotNull
  @Override
  public List<ShopData> getAllShops() {
    List<ShopData> shops = Lists.newArrayList();
    
    for (Map<Long, Map<Long, ShopData>> worldShops : shopsMap.values()) {
      for (Map<Long, ShopData> shopData : worldShops.values()) {
        shops.addAll(shopData.values());
      }
    }
    
    return shops;
  }
  
  @Override
  public void forEachShops(@NotNull Consumer<ShopData> consumer) {
    for (Map<Long, Map<Long, ShopData>> worldShops : shopsMap.values()) {
      for (Map<Long, ShopData> chunkShops : worldShops.values()) {
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
  @NotNull
  @Override
  public Optional<Map<Long, ShopData>> getShopsInChunk(@NotNull Chunk c) {
    return getShopsInChunk(c.getWorld().getName(), c.getX(), c.getZ());
  }

  @NotNull
  @Override
  public Optional<Map<Long, ShopData>> getShopsInChunk(@NotNull String world, int chunkX, int chunkZ) {
    Optional<Map<Long, Map<Long, ShopData>>> inWorld = getShopsInWorld(world);
    if (inWorld.isPresent())
      return Optional.ofNullable(inWorld.get().get(Utils.chunkKey(chunkX, chunkZ)));
    
    return Optional.empty();
  }

  /**
   * Returns a hashmap of Chunk - Shop
   *
   * @param world The name of the world (case sensitive) to get the list of shops from
   * @return a hashmap of Chunk - Shop
   */
  @NotNull
  @Override
  public Optional<Map<Long, Map<Long, ShopData>>> getShopsInWorld(@NotNull String world) {
    return Optional.ofNullable(shopsMap.get(world));
  }
  
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    String world = event.getWorld().getName();
    
    Shop.getManager()
               .getLoadedShops()
               .getOrDefault(world, Collections.emptyMap())
               .values()
               .forEach(blockMap -> blockMap.values().forEach(shop -> Shop.getManager().unload(shop)));
    // Note: we do not actually remove the shop from memory,
    // to ensures it is able to get right data of existence when needed,
    // such as shop cleaner.
    
    event.getWorld().getPlayers().forEach(player -> {
      Shop.getActions().removeAction(player.getUniqueId());
    });
  }
  
  public void delete(@NotNull ContainerShop shop) {
    ShopDeleteEvent shopDeleteEvent = new ShopDeleteEvent(shop, false);
    if (Util.fireCancellableEvent(shopDeleteEvent)) {
      Util.debug("Shop deletion was canceled because a plugin canceled it.");
      return;
    }
    
    Optional<Map<Long, ShopData>> inChunk = getShopsInChunk(shop.getLocation().chunk());
    if (inChunk.isPresent() && !inChunk.get().isEmpty())
      inChunk.get().remove(shop.getLocation().blockKey());
    
    Shop.getManager().unload(shop);
    
    // Delete the signs around it
    for (Sign s : shop.getShopSigns())
      s.getBlock().setType(Material.AIR);
    
    // Delete it from the database
    try {
      QuickShop.instance().getDatabaseHelper().deleteShop(shop.getLocation().x(), shop.getLocation().y(), shop.getLocation().z(), shop.getLocation().worldName());
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      if (BaseConfig.refundable)
        QuickShop.instance().getEconomy().deposit(shop.getOwner(), BaseConfig.refundCost);
    }
  }
  
  public void delete(@NotNull ShopData data) throws SQLException {
    ShopViewer viewer =
        Shop.getManager().getLoadedShopAt(data.world(), data.x(), data.y(), data.z());
    
    if (viewer.isPresent()) {
      delete(viewer.get());
    } else {
      QuickShop.instance().getDatabaseHelper().deleteShop(data.x(), data.y(), data.z(), data.world());
    }
  }
  
  @EventHandler(priority = EventPriority.MONITOR)
  public void onChunkLoad(ChunkLoadEvent event) {
    if (event.isNewChunk())
      return;

    Optional<Map<Long, ShopData>> inChunk = getShopsInChunk(event.getChunk());
    
    if (inChunk.isPresent() && !inChunk.get().isEmpty())
      Bukkit.getScheduler().runTask(QuickShop.instance(), () -> {
        inChunk.get().values().forEach(shop -> {
          try {
            if (Util.canBeShop(event.getWorld(), shop.x(), shop.y(), shop.z())) {
              Shop.getManager().load(event.getWorld(), shop);
            } else {
              QuickShop.instance().getDatabaseHelper().deleteShop(shop.x(), shop.y(), shop.z(), shop.world());
            }
          } catch (Throwable t) {
            t.printStackTrace();
          }
        });
      });
  }
  
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onChunkUnload(ChunkUnloadEvent e) {
    @Nullable Map<Long, ContainerShop> inChunk = Shop.getManager().getLoadedShopsInChunk(e.getChunk());

    if (inChunk != null && !inChunk.isEmpty())
      Bukkit.getScheduler().runTask(QuickShop.instance(), () -> {
        inChunk.values().forEach(shop -> Shop.getManager().unload(shop));
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
  
  public void loadShopsFor(@NotNull ResultSet set, @NotNull World world) throws SQLException, JsonSyntaxException, InvalidConfigurationException {
    Map<Long, Map<Long, ShopData>> inWorld = shopsMap.computeIfAbsent(world.getName(), s -> new HashMap<>(3));
    
    long foundShops = 0;
    long loadedShops = 0;
    long durTotalShopsNano = 0;
    
    while (set.next()) {
      long onPerShop = System.nanoTime();
      ShopData data = new ShopData(set);
      
      if (!data.world().equals(world.getName())) {
        durTotalShopsNano = System.nanoTime() - onPerShop;
        continue;
      }
      
      if (!canLoad(data)) {
        Util.debug("Somethings gone wrong, skipping the loading...");
        durTotalShopsNano = System.nanoTime() - onPerShop;
        continue;
      }
      
      Map<Long, ShopData> inChunk =
          inWorld.computeIfAbsent(Utils.chunkKey(data.x() >> 4, data.z() >> 4), s -> Maps.newHashMap());
      
      if (Util.isChunkLoaded(world, data.x() >> 4, data.z() >> 4)) {
        // Load to World
        if (Util.canBeShop(world, data.x(), data.y(), data.z())) {
          loadedShops++;
          Shop.getManager().load(world, data);
          inChunk.put(Utils.blockKey(data.x(), data.y(), data.z()), data);
        } else {
          QuickShop.instance().getDatabaseHelper().deleteShop(data.x(), data.y(), data.z(), data.world());
        }
      } else {
        inChunk.put(Utils.blockKey(data.x(), data.y(), data.z()), data);
      }
      
      foundShops++;
      durTotalShopsNano = System.nanoTime() - onPerShop;
    }
    
    if (loadedShops > 0) {
      long averagePerShop = durTotalShopsNano / loadedShops;
      
      ShopLogger.instance().info(
          "Loaded " + ChatColor.GREEN + loadedShops + ChatColor.RESET + " of " + foundShops +
          " shops in " + world.getName() +
          " (Total: " + (durTotalShopsNano / 1000000) + "ms, Avg Per: " + averagePerShop + " ns)");
    } else {
      if (foundShops > 0)
        ShopLogger.instance().info("Found " + ChatColor.GREEN + loadedShops + ChatColor.RESET +
            " shops in " + world.getName() +
            " and would be loaded when needed");
      else
        ShopLogger.instance().info("No shop was found in " + world.getName());
    }
  }

  private static boolean canLoad(@NotNull ShopData info) {
    if (info.item() == null)
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
