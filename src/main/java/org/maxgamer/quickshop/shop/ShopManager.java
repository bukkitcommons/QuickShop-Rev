package org.maxgamer.quickshop.shop;

import com.google.common.collect.Maps;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.material.Sign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.event.ShopCreateEvent;
import org.maxgamer.quickshop.event.ShopPreCreateEvent;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.data.ShopCreator;
import org.maxgamer.quickshop.shop.api.data.ShopLocation;
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
  private final Map<String, Map<Long, Map<Long, Shop>>> loadedShops = Maps.newHashMap();
  
  /**
   * Adds a shop to the world. Does NOT require the chunk or world to be loaded Call shop.onLoad by
   * yourself
   *
   * @param world The name of the world
   * @param shop The shop to add
   */
  public void load(@NotNull Shop shop) {
    if (shop.isLoaded())
      return;
    
    @NotNull ShopLocation location = shop.getLocation();
    
    Map<Long, Map<Long, Shop>> inWorld =
        loadedShops.computeIfAbsent(location.worldName(), s -> new HashMap<>(3));
    
    Map<Long, Shop> inChunk =
        inWorld.computeIfAbsent(Util.chunkKey(location.x() >> 4, location.z() >> 4), s -> Maps.newHashMap());
    
    inChunk.put(location.blockKey(), shop);
    shop.onLoad();
  }

  /**
   * Checks other plugins to make sure they can use the chest they're making a shop.
   *
   * @param player The player to check
   * @param block The block to check
   * @return True if they're allowed to place a shop there.
   */
  public static boolean canBuildShop(@NotNull Player player, @NotNull Block block) {
    try {
      if (QuickShop.instance().isLimit()) {
        UUID uuid = player.getUniqueId();
        long owned = ShopLoader.instance().getAllShop()
            .stream()
            .filter(shop -> shop.getOwner().equals(uuid))
            .count();
        
        int max = QuickShop.instance().getShopLimit(player);
        
        if (owned >= max) {
          player.sendMessage(
              MsgUtil.getMessage("reached-maximum-can-create", player,
                  String.valueOf(owned), String.valueOf(max)));
          
          return false;
        }
      }
      
      QuickShop.instance().getCompatibilityTool().toggleProtectionListeners(false, player);
      if (!QuickShop.instance().getPermissionChecker().canBuild(player, block)) {
        Util.debug("PermissionChecker canceled shop creation");
        return false;
      }
      
      ShopPreCreateEvent event = new ShopPreCreateEvent(player, block.getLocation());
      if (Util.fireCancellableEvent(event))
        return false;
      
    } finally {
      QuickShop.instance().getCompatibilityTool().toggleProtectionListeners(true, player);
    }

    return true;
  }

  /**
   * Removes all shops from memory and the world. Does not delete them from the database. Call this
   * on plugin disable ONLY.
   */
  public void clear() {
    viewLoadedShops(shops -> shops.forEach(Shop::onUnload));
    loadedShops.clear();
  }
  
  public void viewLoadedShops(Consumer<Collection<Shop>> con) {
    loadedShops.values()
    .forEach(inChunk -> inChunk.values()
        .forEach(blockMap -> con.accept(blockMap.values())));
  }

  /**
   * Create a shop use Shop and Info object.
   *
   * @param shop The shop object
   * @param info The info object
   */
  public void createShop(@NotNull Shop shop, @NotNull ShopCreator info) {
    Player player = Bukkit.getPlayer(shop.getOwner());
    if (player == null)
      return;
    
    ShopCreateEvent event = new ShopCreateEvent(shop, player);
    if (Util.fireCancellableEvent(event))
      return;
    
    // Create sign
    if (info.sign() != null /*&& QuickShop.instance().getConfig().getBoolean("shop.auto-sign")*/) {
      if (!Util.isAir(info.sign().getType()) &&
          !(Util.isWallSign(info.sign().getType()) &&
              Arrays.stream(((org.bukkit.block.Sign) info.sign().getState()).getLines())
              .allMatch(String::isEmpty))) {
        
        Util.debug("Sign cannot placed cause no enough space(Not air block)");
        return;
      }
      
      boolean isWaterLogged = info.sign().getType() == Material.WATER;
      info.sign().setType(Util.getSignMaterial());
      
      BlockState signState = info.sign().getState();
      if (isWaterLogged) {
        try {
          BlockData data = signState.getBlockData();
          if (data instanceof Waterlogged) {
            Waterlogged waterable = (Waterlogged) data;
            waterable.setWaterlogged(true);
          }
        } catch (Throwable t) {
          ;
        }
      }
      
      BlockFace chestFace = info.location().block().getFace(info.sign());
      assert chestFace != null;
      
      try {
        org.bukkit.block.data.type.WallSign signData =
            (org.bukkit.block.data.type.WallSign) signState.getBlockData();
        
        signData.setFacing(chestFace);
        signState.setBlockData(signData);
      } catch (Throwable t) {
        Sign sign = (Sign) signState.getData();
        sign.setFacingDirection(chestFace);
        signState.setData(sign);
      }
      
      signState.update(true);
      shop.setSignText();
    }
    
    @NotNull ShopLocation location = shop.getLocation();
    try {
      QuickShop
        .instance()
        .getDatabaseHelper()
        .createShop(shop.getModerator().serialize(),
                    shop.getPrice(), shop.getItem(),
                    shop.isUnlimited() ? 1 : 0, shop.getShopType().toID(),
                    location.worldName(),
                    location.x(), location.y(), location.z());
      
      Map<Long, Shop> inChunk =
          ShopLoader
            .instance()
            .getAllShops()
            .computeIfAbsent(location.world().getName(),
                s -> new HashMap<>(3))
            .computeIfAbsent(Util.chunkKey(location.x() >> 4, location.z() >> 4),
                s -> Maps.newHashMap());
      
      inChunk.put(location.blockKey(), shop);
      
      load(shop);
    } catch (SQLException error) {
      ShopLogger.instance().warning("SQLException detected, trying to auto fix the database...");
      boolean backupSuccess = Util.backupDatabase();
      try {
        if (backupSuccess) {
          QuickShop.instance().getDatabaseHelper().deleteShop(location.x(), location.y(), location.z(),
              location.worldName());
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
  }
  
  public ShopViewer getLoadedShopAt(@NotNull Block block) {
    switch (block.getType()) {
      case CHEST:
      case TRAPPED_CHEST:
      case ENDER_CHEST:
        return getLoadedShopAt(block.getLocation());
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
  public ShopViewer getLoadedShopAt(@NotNull ShopLocation loc) {
    @Nullable Map<Long, Shop> inChunk = ShopLoader.instance().getShops(loc.chunk());
    if (inChunk == null)
      return ShopViewer.empty();
    
    return ShopViewer.of(inChunk.get(loc.blockKey()));
  }
  
  public ShopViewer getLoadedShopAt(Location loc) {
    @Nullable Map<Long, Shop> inChunk = ShopLoader.instance().getShops(loc.getChunk());
    if (inChunk == null)
      return ShopViewer.empty();
    
    return ShopViewer.of(inChunk.get(Util.blockKey(loc)));
  }
  
  public boolean hasLoadedShopAt(@NotNull ShopLocation loc) {
    @Nullable Map<Long, Shop> inChunk = ShopLoader.instance().getShops(loc.chunk());
    if (inChunk == null)
      return false;
    
    return inChunk.containsKey(loc.blockKey());
  }
  
  public boolean hasLoadedShopAt(@NotNull Location loc) {
    @Nullable Map<Long, Shop> inChunk = ShopLoader.instance().getShops(loc.getChunk());
    if (inChunk == null)
      return false;
    
    return inChunk.containsKey(Util.blockKey(loc));
  }

  public void acceptLoaded(@NotNull ShopLocation loc, @NotNull Consumer<Shop> consumer) {
    @Nullable Map<Long, Shop> inChunk = ShopLoader.instance().getShops(loc.chunk());
    
    if (inChunk != null) {
      Shop shop = inChunk.get(loc.blockKey());
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
  public ShopViewer getLoadedShopFrom(@Nullable Location loc) {
    return loc == null ? ShopViewer.empty() : getLoadedShopFrom(loc.getBlock());
  }

  public ShopViewer getLoadedShopFrom(@NotNull Block block) {
    boolean secondHalf = false;
    Location location = block.getLocation();
    
    switch (block.getType()) {
      case CHEST:
      case TRAPPED_CHEST:
        ShopViewer shopViewer = getLoadedShopAt(location);
        if (shopViewer.isPresent())
          return shopViewer;
        else
          secondHalf = true;
        
      case ENDER_CHEST:
        ShopViewer viewerAt = getLoadedShopAt(location);
        if (viewerAt.isPresent())
          return viewerAt;
        
      default:
        ShopViewer bySign = Util.getShopBySign(block);
        if (bySign.isPresent())
          return bySign;
        
        if (secondHalf) {
          Optional<Location> half = Util.getSecondHalf(block);
          if (half.isPresent()) {
            ShopViewer viewerHalf = getLoadedShopAt(half.get());
            if (viewerHalf.isPresent())
              return viewerHalf;
          }
        }
    }
    
    return ShopViewer.empty();
  }

  /**
   * Removes a shop from the world. Does NOT remove it from the database.
   *
   * @param shop The shop to remove
   */
  public void unload(@NotNull Shop shop) {
    if (!shop.isLoaded())
      return;
    
    @NotNull ShopLocation location = shop.getLocation();
    
    Map<Long, Map<Long, Shop>> inWorld =
        loadedShops.get(location.worldName());
    
    if (inWorld != null) {
      Map<Long, Shop> inChunk =
          inWorld.get(Util.chunkKey(location.x() >> 4, location.z() >> 4));
      
      if (inChunk != null)
        inChunk.remove(location.blockKey());
    }
    
    shop.onUnload();
  }

  /**
   * Get all loaded shops.
   *
   * @return All loaded shops.
   */
  @NotNull
  public Map<String, Map<Long, Map<Long, Shop>>> getLoadedShops() {
    return this.loadedShops;
  }

  /**
   * Get a players all shops.
   *
   * @param playerUUID The player's uuid.
   * @return The list have this player's all shops.
   */
  public @NotNull List<Shop> getPlayerOweShops(@NotNull UUID playerUUID) {
    return ShopLoader.instance().getAllShop().stream().filter(shop -> shop.getOwner().equals(playerUUID))
        .collect(Collectors.toList());
  }

  /**
   * Get the all shops in the world.
   *
   * @param world The world you want get the shops.
   * @return The list have this world all shops
   */
  public @NotNull List<Shop> getShopsInWorld(@NotNull World world) {
    return ShopLoader.instance().getAllShop().stream()
        .filter(shop -> shop.getLocation().worldName().equals(world.getName()))
        .collect(Collectors.toList());
  }
}
