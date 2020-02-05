package org.maxgamer.quickshop.shop.hologram;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopProtectionFlag;
import org.maxgamer.quickshop.utils.Util;

/**
 * @author Netherfoam A display item, that spawns a block above the chest and cannot be interacted
 *         with.
 */
public interface DisplayItem {
  public final static Gson GSON = new Gson();
  
  public static boolean isDisplayItem(@Nullable ItemStack itemStack) {
    return isDisplayItem(itemStack, null);
  }

  /**
   * Check the itemStack is contains protect flag.
   *
   * @param itemStack Target ItemStack
   * @return Contains protect flag.
   */
  public static boolean isDisplayItem(@Nullable ItemStack itemStack, @Nullable Shop shop) {
    if (itemStack == null || !itemStack.hasItemMeta())
      return false;
    
    ItemMeta iMeta = itemStack.getItemMeta();
    if (!iMeta.hasLore())
      return false;
    
    String defaultMark = ShopProtectionFlag.defaultMark();
    // noinspection ConstantConditions
    for (String lore : iMeta.getLore()) {
      try {
        if (!lore.startsWith("{")) {
          continue;
        }
        ShopProtectionFlag shopProtectionFlag = GSON.fromJson(lore, ShopProtectionFlag.class);
        if (shopProtectionFlag == null) {
          continue;
        }
        if (shop == null && defaultMark.equals(shopProtectionFlag.getMark())) {
          return true;
        }
        if (shopProtectionFlag.getShopLocationData() != null) {
          return shop == null ? true : shopProtectionFlag.getShopLocationData().equals(shop.getLocation().toString());
        }
        if (shop == null && shopProtectionFlag.getShopItemStackData() != null) {
          return true;
        }
      } catch (JsonSyntaxException e) {
        // Ignore
      }
    }
    return false;
  }

  /**
   * Create a new itemStack with protect flag.
   *
   * @param itemStack Old itemStack
   * @param shop The shop
   * @return New itemStack with protect flag.
   */
  static ItemStack createGuardItemStack(@NotNull ItemStack itemStack, @NotNull Shop shop) {
    itemStack = new ItemStack(itemStack);
    itemStack.setAmount(1);
    ItemMeta iMeta = itemStack.getItemMeta();
    if (BaseConfig.displayNameVisible) {
      if (iMeta.hasDisplayName()) {
        iMeta.setDisplayName(iMeta.getDisplayName());
      } else {
        iMeta.setDisplayName(Util.getItemStackName(itemStack));
      }
    } else {
      iMeta.setDisplayName(null);
    }
    java.util.List<String> lore = new ArrayList<>();
    ShopProtectionFlag shopProtectionFlag = ShopProtectionFlag.create(itemStack, shop);
    String protectFlag = GSON.toJson(shopProtectionFlag);
    for (int i = 0; i < 21; i++) {
      lore.add(protectFlag); // Create 20 lines lore to make sure no stupid plugin accident remove
                             // mark.
    }
    iMeta.setLore(lore);
    itemStack.setItemMeta(iMeta);
    return itemStack;
  }

  /**
   * Check target Entity is or not a QuickShop display Entity.
   *
   * @param entity Target entity
   * @return Is or not
   */
  boolean isDisplayItem(Entity entity);

  /** Fix the display moved issue. */
  void fixPosition();

  /** Remove the display entity. */
  void remove();

  /**
   * Remove this shop's display in the whole world.(Not whole server)
   *
   * @return Success
   */
  boolean removeDupe();

  /** Respawn the displays, if it not exist, it will spawn new one. */
  void respawn();

  /**
   * Add the protect flags for entity or entity's hand item. Target entity will got protect by
   * QuickShop
   *
   * @param entity Target entity
   */
  void safeGuard(Entity entity);

  /** Spawn new Displays */
  void spawn();

  /**
   * Get the display entity
   *
   * @return Target entity
   */
  Entity getDisplay();

  /**
   * Get display should at location. Not display current location.
   *
   * @return Should at
   */
  Location getDisplayLocation();

  /**
   * Check the display is or not already spawned
   *
   * @return Spawned
   */
  boolean isSpawned();

  boolean pendingRemoval();

  boolean isPendingRemoval();
}
