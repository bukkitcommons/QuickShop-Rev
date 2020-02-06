package org.maxgamer.quickshop.shop;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.event.ShopCreateEvent;
import org.maxgamer.quickshop.event.ShopPreCreateEvent;
import org.maxgamer.quickshop.event.ShopPurchaseEvent;
import org.maxgamer.quickshop.event.ShopSuccessPurchaseEvent;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopChunk;
import org.maxgamer.quickshop.shop.api.ShopModerator;
import org.maxgamer.quickshop.shop.api.ShopType;
import org.maxgamer.quickshop.shop.api.data.ShopAction;
import org.maxgamer.quickshop.shop.api.data.ShopCreator;
import org.maxgamer.quickshop.shop.api.data.ShopData;
import org.maxgamer.quickshop.shop.api.data.ShopSnapshot;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

public class ShopManager {
  /*
   * Singleton
   */
  private static class LazySingleton {
    private static final ShopManager INSTANCE = new ShopManager();
  }
  
  private ShopManager() {}
  
  public static ShopManager instance() {
    return LazySingleton.INSTANCE;
  }

  /*
   * Shop containers
   */
  private final Map<String, Map<ShopChunk, Map<Location, Shop>>> shops = Maps.newHashMap();
  private final Set<Shop> loadedShops = QuickShop.instance().isEnabledAsyncDisplayDespawn() ? Sets.newConcurrentHashSet() : Sets.newHashSet();

  /**
   * Adds a shop to the world. Does NOT require the chunk or world to be loaded Call shop.onLoad by
   * yourself
   *
   * @param world The name of the world
   * @param shop The shop to add
   */
  public void addShop(@NotNull String world, @NotNull Shop shop) {
    Map<ShopChunk, Map<Location, Shop>> inWorld =
        this.getShops().computeIfAbsent(world, k -> new HashMap<>(3));
    // There's no world storage yet. We need to create that hashmap.
    // Put it in the data universe
    // Calculate the chunks coordinates. These are 1,2,3 for each chunk, NOT
    // location rounded to the nearest 16.
    int x = (int) Math.floor((shop.getLocation().getBlockX()) / 16.0);
    int z = (int) Math.floor((shop.getLocation().getBlockZ()) / 16.0);
    // Get the chunk set from the world info
    ShopChunk shopChunk = new ShopChunk(world, x, z);
    Map<Location, Shop> inChunk = inWorld.computeIfAbsent(shopChunk, k -> new HashMap<>(1));
    // That chunk data hasn't been created yet - Create it!
    // Put it in the world
    // Put the shop in its location in the chunk list.
    inChunk.put(shop.getLocation(), shop);
    // shop.onLoad();
  }

  /**
   * Checks other plugins to make sure they can use the chest they're making a shop.
   *
   * @param p The player to check
   * @param b The block to check
   * @param bf The blockface to check
   * @return True if they're allowed to place a shop there.
   */
  public boolean canBuildShop(@NotNull Player p, @NotNull Block b, @NotNull BlockFace bf) {
    try {
      QuickShop.instance().getCompatibilityTool().toggleProtectionListeners(false, p);

      if (QuickShop.instance().isLimit()) {
        int owned = 0;
        if (!QuickShop.instance().getConfig().getBoolean("limits.old-algorithm")) {
          for (Map<ShopChunk, Map<Location, Shop>> shopmap : getShops().values()) {
            for (Map<Location, Shop> shopLocs : shopmap.values()) {
              for (Shop shop : shopLocs.values()) {
                if (shop.getOwner().equals(p.getUniqueId()) && !shop.isUnlimited()) {
                  owned++;
                }
              }
            }
          }
        } else {
          Iterator<Shop> it = getShopIterator();
          while (it.hasNext()) {
            if (it.next().getOwner().equals(p.getUniqueId())) {
              owned++;
            }
          }
        }

        int max = QuickShop.instance().getShopLimit(p);
        if (owned + 1 > max) {
          p.sendMessage(MsgUtil.getMessage("reached-maximum-can-create", p, String.valueOf(owned),
              String.valueOf(max)));
          return false;
        }
      }
      if (!QuickShop.instance().getPermissionChecker().canBuild(p, b)) {
        Util.debugLog("PermissionChecker canceled shop creation");
        return false;
      }
      ShopPreCreateEvent spce = new ShopPreCreateEvent(p, b.getLocation());
      Bukkit.getPluginManager().callEvent(spce);
      if (Util.fireCancellableEvent(spce)) {
        return false;
      }
    } finally {
      QuickShop.instance().getCompatibilityTool().toggleProtectionListeners(true, p);
    }

    return true;
  }

  /**
   * Removes all shops from memory and the world. Does not delete them from the database. Call this
   * on plugin disable ONLY.
   */
  public void clear() {
    if (BaseConfig.displayItems) {
      for (World world : Bukkit.getWorlds()) {
        for (Chunk chunk : world.getLoadedChunks()) {
          @Nullable Map<Location, Shop> inChunk = this.getShops(chunk);
          if (inChunk == null || inChunk.isEmpty()) {
            continue;
          }
          for (Shop shop : inChunk.values()) {
            shop.onUnload();
          }
        }
      }
    }
    ShopActionManager.instance().getActions().clear(); // FIXME
    this.shops.clear();
  }

  /**
   * Create a shop use Shop and Info object.
   *
   * @param shop The shop object
   * @param info The info object
   */
  public void createShop(@NotNull Shop shop, @NotNull ShopCreator info) {
    Player player = Bukkit.getPlayer(shop.getOwner());
    if (player == null) {
      throw new IllegalStateException("The owner creating the shop is offline or not exist");
    }
    ShopCreateEvent ssShopCreateEvent = new ShopCreateEvent(shop, player);
    if (Util.fireCancellableEvent(ssShopCreateEvent)) {
      Util.debugLog("Cancelled by plugin");
      return;
    }
    Location loc = shop.getLocation();
    try {
      // Write it to the database
      QuickShop.instance().getDatabaseHelper().createShop(shop.getModerator().serialize(),
          shop.getPrice(), shop.getItem(), (shop.isUnlimited() ? 1 : 0), shop.getShopType().toID(),
          Objects.requireNonNull(loc.getWorld()).getName(), loc.getBlockX(), loc.getBlockY(),
          loc.getBlockZ());
      // Add it to the world
      addShop(loc.getWorld().getName(), shop);
    } catch (SQLException error) {
      ShopLogger.instance().warning("SQLException detected, trying to auto fix the database...");
      boolean backupSuccess = Util.backupDatabase();
      try {
        if (backupSuccess) {
          QuickShop.instance().getDatabaseHelper().deleteShop(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
              loc.getWorld().getName());
        } else {
          ShopLogger.instance()
          .warning("Failed to backup the database, all changes will revert after a reboot.");
        }
      } catch (SQLException error2) {
        // Failed removing
        ShopLogger.instance()
        .warning("Failed to autofix the database, all changes will revert after a reboot.");
        error2.printStackTrace();
      }
      error.printStackTrace();
    }
    // Create sign
    if (info.sign() != null && QuickShop.instance().getConfig().getBoolean("shop.auto-sign")) {
      if (!Util.isAir(info.sign().getType())) {
        Util.debugLog("Sign cannot placed cause no enough space(Not air block)");
        return;
      }
      boolean isWaterLogged = false;
      if (info.sign().getType() == Material.WATER) {
        isWaterLogged = true;
      }

      info.sign().setType(Util.getSignMaterial());
      BlockState bs = info.sign().getState();
      if (isWaterLogged) {
        if (bs.getBlockData() instanceof Waterlogged) {
          Waterlogged waterable = (Waterlogged) bs.getBlockData();
          waterable.setWaterlogged(true); // Looks like sign directly put in water
        }
      }
      if (bs.getBlockData() instanceof WallSign) {
        org.bukkit.block.data.type.WallSign signBlockDataType =
            (org.bukkit.block.data.type.WallSign) bs.getBlockData();
        BlockFace bf = info.location().getBlock().getFace(info.sign());
        if (bf != null) {
          signBlockDataType.setFacing(bf);
          bs.setBlockData(signBlockDataType);
        }
      } else {
        ShopLogger.instance().warning("Sign material " + bs.getType().name()
            + " not a WallSign, make sure you using correct sign material.");
      }
      bs.update(true);
      shop.setSignText();
    }
  }
  
  public ShopViewer getShopAt(@NotNull Block block) {
    switch (block.getType()) {
      case CHEST:
      case TRAPPED_CHEST:
      case ENDER_CHEST:
        return getShopAt(block.getLocation());
      default:
        return ShopViewer.empty();
    }
  }

  /**
   * Gets a shop in a specific location
   *
   * @param loc The location to get the shop from
   * @return The shop at that location
   */
  public ShopViewer getShopAt(@NotNull Location loc) {
    @Nullable Map<Location, Shop> inChunk = getShops(loc.getChunk());
    if (inChunk == null)
      return ShopViewer.empty();
    
    return ShopViewer.of(inChunk.get(new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
  }

  public void accept(@NotNull Location loc, @NotNull Consumer<Shop> consumer) {
    @Nullable Map<Location, Shop> inChunk = getShops(loc.getChunk());
    
    if (inChunk != null) {
      Shop shop = inChunk.get(new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
      if (shop != null)
        consumer.accept(shop);
    }
  }

  /**
   * Gets a shop in a specific location Include the attached shop, e.g DoubleChest shop.
   *
   * @param loc The location to get the shop from
   * @return The shop at that location
   */
  public ShopViewer getShopFrom(@Nullable Location loc) {
    return loc == null ? ShopViewer.empty() : getShopFrom(loc.getBlock());
  }

  private ShopViewer getShopFrom(@NotNull Block block) {
    boolean secondHalf = false;
    Location location = block.getLocation();
    
    switch (block.getType()) {
      case CHEST:
      case TRAPPED_CHEST:
        ShopViewer shopViewer = getShopAt(location);
        if (shopViewer.isPresent())
          return shopViewer;
        else
          secondHalf = true;
        
      case ENDER_CHEST:
        ShopViewer viewerAt = getShopAt(location);
        if (viewerAt.isPresent())
          return viewerAt;
        
      default:
        ShopViewer bySign = Util.getShopBySign(block);
        if (bySign.isPresent())
          return bySign;
        
        if (secondHalf) {
          Optional<Location> half = Util.getSecondHalf(block);
          if (half.isPresent()) {
            ShopViewer viewerHalf = getShopAt(half.get());
            if (viewerHalf.isPresent())
              return viewerHalf;
          }
        }
    }
    
    return ShopViewer.empty();
  }

  /**
   * Returns a hashmap of Chunk - Shop
   *
   * @param world The name of the world (case sensitive) to get the list of shops from
   * @return a hashmap of Chunk - Shop
   */
  public @Nullable Map<ShopChunk, Map<Location, Shop>> getShops(@NotNull String world) {
    return this.shops.get(world);
  }

  /**
   * Returns a hashmap of Shops
   *
   * @param c The chunk to search. Referencing doesn't matter, only coordinates and world are used.
   * @return Shops
   */
  public @Nullable Map<Location, Shop> getShops(@NotNull Chunk c) {
    // long start = System.nanoTime();
    return getShops(c.getWorld().getName(), c.getX(), c.getZ());
    // long end = System.nanoTime();
    // QuickShopLogger.instance().log(Level.WARNING, "Chunk lookup in " + ((end - start)/1000000.0) +
    // "ms.");
  }

  public @Nullable Map<Location, Shop> getShops(@NotNull String world, int chunkX, int chunkZ) {
    @Nullable Map<ShopChunk, Map<Location, Shop>> inWorld = this.getShops(world);
    if (inWorld == null) {
      return null;
    }
    ShopChunk shopChunk = new ShopChunk(world, chunkX, chunkZ);
    return inWorld.get(shopChunk);
  }

  /**
   * Loads the given shop into storage. This method is used for loading data from the database. Do
   * not use this method to create a shop.
   *
   * @param world The world the shop is in
   * @param shop The shop to load
   */
  public void loadShop(@NotNull String world, @NotNull Shop shop) {
    this.addShop(world, shop);
  }

  /**
   * Removes a shop from the world. Does NOT remove it from the database. * REQUIRES * the world to
   * be loaded Call shop.onUnload by your self.
   *
   * @param shop The shop to remove
   */
  public void unloadShop(@NotNull Shop shop) {
    // shop.onUnload();
    Location loc = shop.getLocation();
    String world = Objects.requireNonNull(loc.getWorld()).getName();
    Map<ShopChunk, Map<Location, Shop>> inWorld = this.getShops().get(world);
    int x = (int) Math.floor((shop.getLocation().getBlockX()) / 16.0);
    int z = (int) Math.floor((shop.getLocation().getBlockZ()) / 16.0);
    ShopChunk shopChunk = new ShopChunk(world, x, z);
    Map<Location, Shop> inChunk = inWorld.get(shopChunk);
    if (inChunk == null) {
      return;
    }
    inChunk.remove(loc);
    // shop.onUnload();
  }

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
  public Collection<Shop> getAllShops() {
    // noinspection unchecked
    Map<String, Map<ShopChunk, Map<Location, Shop>>> worldsMap = Maps.newHashMap(getShops());
    Collection<Shop> shops = new ArrayList<>();
    for (Map<ShopChunk, Map<Location, Shop>> shopMapData : worldsMap.values()) {
      for (Map<Location, Shop> shopData : shopMapData.values()) {
        shops.addAll(shopData.values());
      }
    }
    return shops;
  }

  /**
   * Returns a hashmap of World - Chunk - Shop
   *
   * @return a hashmap of World - Chunk - Shop
   */
  @NotNull
  public Map<String, Map<ShopChunk, Map<Location, Shop>>> getShops() {
    return this.shops;
  }

  /**
   * Get all loaded shops.
   *
   * @return All loaded shops.
   */
  @NotNull
  public Set<Shop> getLoadedShops() {
    return this.loadedShops;
  }

  /**
   * Get a players all shops.
   *
   * @param playerUUID The player's uuid.
   * @return The list have this player's all shops.
   */
  public @NotNull List<Shop> getPlayerAllShops(@NotNull UUID playerUUID) {
    return getAllShops().stream().filter(shop -> shop.getOwner().equals(playerUUID))
        .collect(Collectors.toList());
  }

  /**
   * Get the all shops in the world.
   *
   * @param world The world you want get the shops.
   * @return The list have this world all shops
   */
  public @NotNull List<Shop> getShopsInWorld(@NotNull World world) {
    return getAllShops().stream()
        .filter(shop -> Objects.equals(shop.getLocation().getWorld(), world))
        .collect(Collectors.toList());
  }

  public class ShopIterator implements Iterator<Shop> {
    private Iterator<Map<Location, Shop>> chunks;
    private Iterator<Shop> shops;
    private Iterator<Map<ShopChunk, Map<Location, Shop>>> worlds;

    public ShopIterator() {
      // noinspection unchecked
      Map<String, Map<ShopChunk, Map<Location, Shop>>> worldsMap = Maps.newHashMap(getShops());
      
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
}
