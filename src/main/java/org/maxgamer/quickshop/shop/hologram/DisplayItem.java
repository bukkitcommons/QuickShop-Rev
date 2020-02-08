package org.maxgamer.quickshop.shop.hologram;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.SneakyThrows;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopProtectionFlag;
import org.maxgamer.quickshop.shop.hologram.impl.EntityDisplayItem;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

/**
 * @author Netherfoam A display item, that spawns a block above the chest and cannot be interacted
 *         with.
 */
public interface DisplayItem {
  public final static Gson GSON = new Gson();
  
  public static boolean isDisplayItem(@Nullable ItemStack itemStack) {
    return isDisplayItem(itemStack, null);
  }
  
  @SneakyThrows
  public static void fixesDisplayItem(@Nullable Item item) {
    ItemStack itemStack = item.getItemStack();
    if (itemStack == null || !itemStack.hasItemMeta())
      return;
    
    ItemMeta iMeta = itemStack.getItemMeta();
    if (!iMeta.hasLore())
      return;
    
    for (String lore : iMeta.getLore()) {
      try {
        if (!lore.startsWith("{")) {
          continue;
        }
        ShopProtectionFlag shopProtectionFlag = GSON.fromJson(lore, ShopProtectionFlag.class);
        if (shopProtectionFlag == null)
          continue;
        
        if (shopProtectionFlag.getShopLocationData() != null) {
          ShopViewer viewer =
              ShopManager.instance().getLoadedShopAt(deserialize(shopProtectionFlag.getShopLocationData()));
          
          viewer.ifPresent(shop -> {
            if (shop.getDisplay().getDisplayLocation().distance(item.getLocation()) > 0.6) {
              item.remove();
              Util.debug("Removed a duped item display entity.1");
              shop.checkDisplay();
              return;
            }
            
            if (shop.getDisplay().getDisplay() == null)
              return;
            
            if (!shop.getDisplay().getDisplay().getUniqueId().equals(item.getUniqueId())) {
              item.remove();
              Util.debug("Removed a duped item display entity.2");
              return;
            }
            
            if (((EntityDisplayItem) shop.getDisplay()).data().type().entityType() != item.getType()) {
              item.remove();
              Util.debug("Removed a duped item display entity.3");
              return;
            }
          });
        } else if (shopProtectionFlag.getShopItemStackData() != null) {
          ItemStack displayItem = Util.deserialize(shopProtectionFlag.getShopItemStackData());
          
          if (!QuickShop.instance().getItemMatcher().matches(itemStack, displayItem)) {
            item.remove();
            Util.debug("Removed a duped item display entity.");
          }
        }
      } catch (JsonSyntaxException e) {
        return;
      }
    }
  }
  
  static Location deserialize(@NotNull String location) {
    Util.debug("will deserilize: " + location);
    String[] sections = location.split(",");
    String worldName = StringUtils.substringBetween(sections[0], "{name=", "}");
    String x = sections[1].substring(2);
    String y = sections[2].substring(2);
    String z = sections[3].substring(2);
    
    return new Location(Bukkit.getWorld(worldName), Double.valueOf(x), Double.valueOf(y), Double.valueOf(z));
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
    
    ItemMeta meta = itemStack.getItemMeta();
    if (BaseConfig.displayNameVisible) {
      if (meta.hasDisplayName())
        meta.setDisplayName(meta.getDisplayName());
      else
        meta.setDisplayName(Util.getItemStackName(itemStack));
    } else {
      meta.setDisplayName("");
    }
    
    ShopProtectionFlag shopProtectionFlag = ShopProtectionFlag.create(itemStack, shop);
    meta.setLore(Collections.singletonList(GSON.toJson(shopProtectionFlag)));
    
    itemStack.setItemMeta(meta);
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

  /** Respawn the displays, if it not exist, it will spawn new one. */
  void respawn();

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
