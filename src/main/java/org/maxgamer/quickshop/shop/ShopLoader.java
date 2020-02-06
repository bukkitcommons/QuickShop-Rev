package org.maxgamer.quickshop.shop;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopModerator;
import org.maxgamer.quickshop.shop.api.ShopType;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.ShopLogger;

/** A class allow plugin load shops fast and simply. */
public class ShopLoader implements Listener {
  
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    String world = event.getWorld().getName();
    
    ShopManager.instance()
               .getLoadedShops()
               .stream()
               .filter(shop -> shop.getLocation().getWorld().getName().equals(world))
               .forEach(shop -> ShopManager.instance().unload(shop));
  }
  
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldLoad(WorldLoadEvent event) {
    loadShopsForWorld(event.getWorld());
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
            ShopManager.instance().load(data.world(), shop);
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
  
  public static void forEachShopFromDatabase(@NotNull Consumer<org.maxgamer.quickshop.shop.api.Shop> consumer) {
    try {
      ShopLogger.instance().info("Loading shops from the database..");
      ResultSet rs = QuickShop.instance().getDatabaseHelper().selectAllShops();
      
      while (rs.next()) {
        ShopDatabaseInfo data = new ShopDatabaseInfo(rs);
        
        if (!canLoad(data)) {
          Util.debugLog("Somethings gone wrong, skipping the loading...");
          continue;
        }
        
        Shop shop = new ContainerShop(
            new Location(Bukkit.getWorld(data.world()), data.x(), data.y(), data.z()),
            data.price(), data.item(),
            data.moderators(), data.unlimited(), data.type());
        
        consumer.accept(shop);
      }
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
