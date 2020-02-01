/*
 * This file is a part of project QuickShop, the name is ShopLoader.java Copyright (C) Ghost_chu
 * <https://github.com/Ghost-chu> Copyright (C) Bukkit Commons Studio and contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.maxgamer.quickshop.shop.impl;

import com.google.common.collect.Lists;
import com.google.common.graph.ElementOrder.Type;
import com.google.gson.JsonSyntaxException;
import java.io.Serializable;
import java.security.interfaces.RSAKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.ShopModerator;
import org.maxgamer.quickshop.shop.ShopType;
import org.maxgamer.quickshop.utils.Timer;
import org.maxgamer.quickshop.utils.Util;

/** A class allow plugin load shops fast and simply. */
public class ShopLoader {
  /**
   * Load all shops
   *
   * @param worldName The world name
   */
  public void loadShops(@Nullable String worldName) {
    long onLoad = System.currentTimeMillis();
    
    try {
      QuickShop.instance().getLogger().info("Loading shops from the database..");
      long onFetch = System.currentTimeMillis();
      ResultSet rs = QuickShop.instance().getDatabaseHelper().selectAllShops();
      long durFetch = System.currentTimeMillis() - onFetch;
      long fetchSize = rs.getFetchSize();
      QuickShop.instance().getLogger().info("Fetched" + fetchSize + "shops from database by " + durFetch + "ms");
      
      long loadedShops = 0;
      long durTotalShopsNano = 0;
      
      while (rs.next()) {
        long onPerShop = System.nanoTime();
        ShopDatabaseInfo data = new ShopDatabaseInfo(rs);
        
        boolean ignoreWorlds = worldName == null;
        if (!ignoreWorlds && !data.getWorld().equals(worldName)) {
          durTotalShopsNano = System.nanoTime() - onPerShop;
          continue;
        }
        
        if (!canLoad(data)) {
          Util.debugLog("Somethings gone wrong, skipping the loading...");
          durTotalShopsNano = System.nanoTime() - onPerShop;
          continue;
        }
        
        Shop shop = new ContainerShop(
            new Location(Bukkit.getWorld(data.getWorld()), data.getX(), data.getY(), data.getZ()),
            data.getPrice(), data.getItem(),
            data.getModerators(), data.isUnlimited(), data.getType());
        
        // Load to RAM
        QuickShop.instance().getShopManager().loadShop(data.getWorld(), shop);
        
        if (Util.isChunkLoaded(shop.getLocation())) {
          // Load to World
          if (Util.canBeShop(shop.getLocation().getBlock())) {
            loadedShops++;
            shop.onLoad();
            
          } else {
            Util.debugLog("Target block can't be a shop, removing it from the database...");
            // shop.delete();
            QuickShop.instance().getShopManager().unloadShop(shop);
            QuickShop.instance().getDatabaseHelper().deleteShop(
                shop.getLocation().getBlockX(),
                shop.getLocation().getBlockY(),
                shop.getLocation().getBlockZ(),
                shop.getLocation().getWorld().getName());
          }
        }
        
        durTotalShopsNano = System.nanoTime() - onPerShop;
      }
      
      long durLoad = System.currentTimeMillis() - onLoad;
      long averagePerShop = durTotalShopsNano / loadedShops;
      
      QuickShop.instance().getLogger().info(
          "Successfully loaded " + loadedShops + " of " + fetchSize + " shops!" +
          "(Total: " + durLoad + "ms, Fetch: " + durFetch + "ms," +
          " Load: " + (durTotalShopsNano / 1000000) + "ms, Avg Per: " + averagePerShop + "ns)");
      
    } catch (Throwable t) {
      exceptionHandler(t, null);
    }
  }

  public void loadShops() {
    loadShops(null);
  }

  private boolean canLoad(@NotNull ShopDatabaseInfo info) {
    if (info.item() == null) {
      Util.debugLog("Shop ItemStack is null");
      return false;
    }
    if (info.item().getType() == Material.AIR) {
      Util.debugLog("Shop ItemStack type can't be AIR");
      return false;
    }
    if (info.world() == null) {
      Util.debugLog("Shop World is null");
      return false;
    }
    if (info.moderators() == null) {
      Util.debugLog("Shop Owner is null");
      return false;
    }
    //if (Bukkit.getOfflinePlayer(shop.getOwner()).getName() == null) {
    //  Util.debugLog("Shop owner not exist on this server, did you reset the playerdata?");
    //}
    return true;
  }

  private static @Nullable ItemStack deserializeItem(@NotNull String itemConfig) {
    try {
      return Util.deserialize(itemConfig);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
      QuickShop.instance().getLogger().warning(
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
  public class ShopDatabaseInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private ItemStack item;
    private ShopModerator moderators;
    private double price;
    private ShopType type;
    private boolean unlimited;
    private String world;
    private int x;
    private int y;
    private int z;
    
    public ShopDatabaseInfo(ResultSet rs) {
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
        
      } catch (SQLException sql) {
        exceptionHandler(sql, rs); // FIXME
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
  
  private static void exceptionHandler(@NotNull Throwable throwable, @NotNull ResultSet set) {
    Logger logger = QuickShop.instance().getLogger();
    
    logger.warning("########## FAILED TO LOAD A SHOP FROM DB ##########");
    logger.warning("  >> Error Info:");
    String message = throwable.getMessage();
    message = message == null ? "~NULL~" : message;
    logger.warning("  > " + message);
    
    logger.warning("  >> Error Trace");
    throwable.printStackTrace();
    
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
    
    logger.warning("  >> Nested Errors");
    for (int i = 0; i < nested.size(); i++) {
      logger.warning("  >  Id " + i);
      nested.get(i).printStackTrace();
      logger.warning("");
    }

    logger.warning("#######################################");
  }
}
