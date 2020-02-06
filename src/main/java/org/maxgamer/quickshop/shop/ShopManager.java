package org.maxgamer.quickshop.shop;

import com.google.common.collect.Sets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import org.bukkit.block.Sign;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.event.ShopCreateEvent;
import org.maxgamer.quickshop.event.ShopDeleteEvent;
import org.maxgamer.quickshop.event.ShopPreCreateEvent;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.data.ShopCreator;
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
  private final Set<Shop> loadedShops = QuickShop.instance().isEnabledAsyncDisplayDespawn() ? Sets.newConcurrentHashSet() : Sets.newHashSet();
  
  /**
   * Adds a shop to the world. Does NOT require the chunk or world to be loaded Call shop.onLoad by
   * yourself
   *
   * @param world The name of the world
   * @param shop The shop to add
   */
  public void load(@NotNull Shop shop) {
    loadedShops.add(shop);
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
        Util.debugLog("PermissionChecker canceled shop creation");
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
    loadedShops.forEach(Shop::onUnload);
    loadedShops.clear();
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
      load(shop);
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
    @Nullable Map<Long, Shop> inChunk = ShopLoader.instance().getShops(loc.getChunk());
    if (inChunk == null)
      return ShopViewer.empty();
    
    return ShopViewer.of(inChunk.get(Util.blockKey(loc)));
  }
  
  public boolean hasShopAt(@NotNull Location loc) {
    @Nullable Map<Long, Shop> inChunk = ShopLoader.instance().getShops(loc.getChunk());
    if (inChunk == null)
      return false;
    
    return inChunk.containsKey(Util.blockKey(loc));
  }

  public void accept(@NotNull Location loc, @NotNull Consumer<Shop> consumer) {
    @Nullable Map<Long, Shop> inChunk = ShopLoader.instance().getShops(loc.getChunk());
    
    if (inChunk != null) {
      Shop shop = inChunk.get(Util.blockKey(loc));
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
   * Removes a shop from the world. Does NOT remove it from the database. * REQUIRES * the world to
   * be loaded Call shop.onUnload by your self.
   *
   * @param shop The shop to remove
   */
  public void unload(@NotNull Shop shop) {
    loadedShops.remove(shop);
    shop.onUnload();
  }
  
  public void delete(@NotNull Shop shop) {
    ShopDeleteEvent shopDeleteEvent = new ShopDeleteEvent(shop, false);
    if (Util.fireCancellableEvent(shopDeleteEvent)) {
      Util.debugLog("Shop deletion was canceled because a plugin canceled it.");
      return;
    }
    
    ShopManager.instance().unload(shop);
    
    // Delete the display item
    if (shop.getDisplay() != null) {
      shop.getDisplay().remove();
    }
    
    // Delete the signs around it
    for (Sign s : shop.getSigns())
      s.getBlock().setType(Material.AIR);
    
    // Delete it from the database
    int x = shop.getLocation().getBlockX();
    int y = shop.getLocation().getBlockY();
    int z = shop.getLocation().getBlockZ();
    String world = shop.getLocation().getWorld().getName();
    
    try {
      QuickShop.instance().getDatabaseHelper().deleteShop(x, y, z, world);
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      if (BaseConfig.refundable)
        QuickShop.instance().getEconomy().deposit(shop.getOwner(), BaseConfig.refundCost);
    }
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
        .filter(shop -> Objects.equals(shop.getLocation().getWorld(), world))
        .collect(Collectors.toList());
  }
}
