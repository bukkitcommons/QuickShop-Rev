package org.maxgamer.quickshop.shop.hologram.impl;

import java.util.UUID;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.event.ShopDisplayItemDespawnEvent;
import org.maxgamer.quickshop.event.ShopDisplayItemSpawnEvent;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.hologram.DisplayData;
import org.maxgamer.quickshop.shop.hologram.DisplayItem;
import org.maxgamer.quickshop.shop.hologram.DisplayType;
import org.maxgamer.quickshop.utils.Util;
import com.bekvon.bukkit.residence.commands.sublist;

@ToString
public class RealDisplayItem extends EntityDisplayItem implements DisplayItem {
  
  public RealDisplayItem(@NotNull Shop shop) {
    super(shop);
  }

  @Override
  public boolean isDisplayItem(@NotNull Entity entity) {
    if (!(entity instanceof Item)) {
      return false;
    }
    return DisplayItem.isDisplayItem(((Item) entity).getItemStack(), null);
  }

  @Override
  public void remove() {
    super.remove();
    ShopDisplayItemDespawnEvent shopDisplayItemDepawnEvent =
        new ShopDisplayItemDespawnEvent(shop, originalItemStack, DisplayType.REALITEM);
    Bukkit.getPluginManager().callEvent(shopDisplayItemDepawnEvent);
  }

  @Override
  public boolean removeDupe() {
    if (this.entity == null) {
      Util.debugLog("Warning: Trying to removeDupe for a null display shop.");
      return false;
    }

    boolean removed = false;
    // Chunk chunk = shop.getLocation().getChunk();
    for (Entity entity : entity.getNearbyEntities(1.5, 1.5, 1.5)) {
      if (entity.getType() != EntityType.DROPPED_ITEM) {
        continue;
      }
      Item eItem = (Item) entity;
      if (DisplayItem.isDisplayItem(eItem.getItemStack(), this.shop)) {
        Util.debugLog(
            "Removing a duped ItemEntity " + eItem.getUniqueId() + " at " + eItem.getLocation());
        entity.remove();
        removed = true;
      }
    }
    return removed;
  }
  
  @Override
  public void safeGuard(@NotNull Entity entity) {
    if (!(entity instanceof Item)) {
      Util.debugLog("Failed to safeGuard " + entity.getLocation() + ", cause target not a Item");
      return;
    }
    Item item = (Item) entity;
    // Set item protect in the armorstand's hand

    if (BaseConfig.displayNameVisible) {
      item.setCustomName(Util.getItemStackName(this.originalItemStack));
      item.setCustomNameVisible(true);
    }
    item.setPickupDelay(Integer.MAX_VALUE);
    item.setSilent(true);
    item.setPortalCooldown(Integer.MAX_VALUE);
    item.setVelocity(new Vector(0, 0.1, 0));
    item.setCustomNameVisible(false);
  }

  @Override
  public void spawn() {
    if (shop.getLocation().getWorld() == null) {
      Util.debugLog("Canceled the displayItem spawning because the location in the world is null.");
      return;
    }

    if (originalItemStack == null) {
      Util.debugLog("Canceled the displayItem spawning because the ItemStack is null.");
      return;
    }

    synchronized (this) {
      if (entity != null && entity.isValid() && !entity.isDead()) {
        Util.debugLog(
            "Warning: Spawning the Dropped Item for DisplayItem when there is already an existing Dropped Item, May cause a duplicated Dropped Item!");
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        for (StackTraceElement trace : traces) {
          Util.debugLog(
              trace.getClassName() + "#" + trace.getMethodName() + "#" + trace.getLineNumber());
        }
      }
      if (!Util.isDisplayAllowBlock(getDisplayLocation().getBlock().getType())) {
        Util.debugLog(
            "Can't spawn the displayItem because there is not an AIR block above the shopblock.");
        return;
      }

      ShopDisplayItemSpawnEvent shopDisplayItemSpawnEvent =
          new ShopDisplayItemSpawnEvent(shop, originalItemStack, DisplayData.create(originalItemStack));
      Bukkit.getPluginManager().callEvent(shopDisplayItemSpawnEvent);
      if (shopDisplayItemSpawnEvent.isCancelled()) {
        Util.debugLog(
            "Canceled the displayItem spawning because a plugin setCancelled the spawning event, usually this is a QuickShop Add on");
        return;
      }
      this.guardedIstack = DisplayItem.createGuardItemStack(this.originalItemStack, this.shop);
      this.entity =
          this.shop.getLocation().getWorld().dropItem(getDisplayLocation(), this.guardedIstack);
      Util.debugLog(
          "Spawned item @ " + this.entity.getLocation() + " with UUID " + this.entity.getUniqueId());
      ((Item) this.entity).setItemStack(this.guardedIstack);
      safeGuard(this.entity);
    }
  }

  @NotNull
  @Override
  public Location getDisplayLocation() {
    return this.shop.getLocation().clone().add(0.5, 1.2, 0.5);
  }
}
