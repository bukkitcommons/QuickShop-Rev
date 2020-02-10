package org.maxgamer.quickshop.shop;

import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopModerator;
import cc.bukkit.shop.data.ShopCreator;
import cc.bukkit.shop.data.ShopData;
import cc.bukkit.shop.data.ShopLocation;
import cc.bukkit.shop.event.ShopCreateEvent;
import cc.bukkit.shop.event.ShopPreCreateEvent;
import cc.bukkit.shop.util.Utils;
import cc.bukkit.shop.viewer.ShopViewer;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.messages.ShopPluginLogger;

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
   * @throws InvalidConfigurationException 
   * @throws JsonSyntaxException 
   */
  public Shop load(@NotNull World world, @NotNull ShopData data) throws JsonSyntaxException, InvalidConfigurationException {
    Map<Long, Map<Long, Shop>> inWorld =
        loadedShops.computeIfAbsent(data.world(), s -> new HashMap<>(3));
    
    Map<Long, Shop> inChunk =
        inWorld.computeIfAbsent(Utils.chunkKey(data.x() >> 4, data.z() >> 4), s -> Maps.newHashMap());
    
    Shop shop = new ContainerShop(
        ShopLocation.from(world, data.x(), data.y(), data.z()),
        data.price(), data.item(),
        data.moderators(), data.unlimited(), data.type());
    
    inChunk.put(Utils.blockKey(data.x(), data.y(), data.z()), shop);
    shop.onLoad();
    
    return shop;
  }
  
  public Shop load(@NotNull Shop shop) {
    Map<Long, Map<Long, Shop>> inWorld =
        loadedShops.computeIfAbsent(shop.getLocation().worldName(), s -> new HashMap<>(3));
    
    Map<Long, Shop> inChunk =
        inWorld.computeIfAbsent(Utils.chunkKey(
            shop.getLocation().x() >> 4, shop.getLocation().z() >> 4), s -> Maps.newHashMap());
    
    inChunk.put(Utils.blockKey(
        shop.getLocation().x(), shop.getLocation().y(), shop.getLocation().z()), shop);
    shop.onLoad();
    
    return shop;
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
        long owned = QuickShopLoader.instance().getAllShops()
            .stream() // ASYNC
            .filter(shop ->
              shop.moderators().getOwner().equals(uuid))
            .count();
        
        int max = QuickShop.instance().getShopLimit(player);
        
        if (owned >= max && Util.canBeShop(block)) {
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
    if (info.sign() != null && BaseConfig.autoSign) {
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
        org.bukkit.material.Sign sign = (org.bukkit.material.Sign) signState.getData();
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
      
      Map<Long, ShopData> inChunk =
          QuickShopLoader
            .instance()
            .getShopsMap()
            .computeIfAbsent(location.worldName(),
                s -> new HashMap<>(3))
            .computeIfAbsent(Utils.chunkKey(location.x() >> 4, location.z() >> 4),
                s -> Maps.newHashMap());
      
      Util.debug("Putting into memory shop database: " + location.toString());
      
      ShopData data = new ShopData(Util.serialize(info.item()), shop.getModerator().serialize(),
          shop.getLocation().worldName(), shop.getShopType(), shop.getPrice(),
          shop.isUnlimited(), shop.getLocation().x(), shop.getLocation().y(), shop.getLocation().z());
      
      inChunk.put(location.blockKey(), data);
      
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
    @Nullable Map<Long, Shop> inChunk = getLoadedShopsInChunk(loc.chunk());
    if (inChunk == null)
      return ShopViewer.empty();
    
    return ShopViewer.of(inChunk.get(loc.blockKey()));
  }
  
  @Nullable
  public Map<Long, Shop> getLoadedShopsInChunk(@NotNull Chunk c) {
    return getLoadedShopsInChunk(c.getWorld().getName(), c.getX(), c.getZ());
  }

  @Nullable
  private Map<Long, Shop> getLoadedShopsInChunk(@NotNull String world, int chunkX, int chunkZ) {
    @Nullable Map<Long, Map<Long, Shop>> inWorld = loadedShops.get(world);
    if (inWorld == null) {
      Util.debug("World map not found: " + world);
      return null;
    }
    
    return inWorld.get(Utils.chunkKey(chunkX, chunkZ));
  }
  
  public ShopViewer getLoadedShopAt(Location loc) {
    @Nullable Map<Long, Shop> inChunk = getLoadedShopsInChunk(loc.getChunk());
    if (inChunk == null) {
      Util.debug("Chunk not found: " + loc);
      return ShopViewer.empty();
    }
    
    return ShopViewer.of(inChunk.get(Utils.blockKey(loc)));
  }
  
  public ShopViewer getLoadedShopAt(String world, int x, int y, int z) {
    @Nullable Map<Long, Shop> inChunk = getLoadedShopsInChunk(world, x >> 4, z >> 4);
    if (inChunk == null)
      return ShopViewer.empty();
    
    return ShopViewer.of(inChunk.get(Utils.blockKey(x, y, z)));
  }
  
  public boolean hasLoadedShopAt(@NotNull Location loc) {
    @Nullable Map<Long, Shop> inChunk = getLoadedShopsInChunk(loc.getChunk());
    if (inChunk == null)
      return false;
    
    return inChunk.containsKey(Utils.blockKey(loc));
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
          inWorld.get(Utils.chunkKey(location.x() >> 4, location.z() >> 4));
      
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
}
