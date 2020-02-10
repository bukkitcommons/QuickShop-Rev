package org.maxgamer.quickshop.shop.hologram.impl;

import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.event.ShopDisplayItemSpawnEvent;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.hologram.DisplayData;
import org.maxgamer.quickshop.shop.hologram.DisplayItem;
import org.maxgamer.quickshop.shop.hologram.DisplayType;
import org.maxgamer.quickshop.utils.Util;

@ToString
public class RealDisplayItem extends EntityDisplayItem implements DisplayItem {
  private static final DisplayData DATA = new DisplayData(DisplayType.DROPPED_ITEM);
  
  public RealDisplayItem(@NotNull Shop shop) {
    super(shop, DATA);
  }
  
  @NotNull
  @Override
  public Location getDisplayLocation() {
    return location == null ? (location = shop.getLocation().bukkit().clone().add(0.5, 1.2, 0.5)) : location;
  }

  @Override
  public boolean isDisplayItem(@NotNull Entity entity) {
    if (entity instanceof Item)
      return DisplayItem.isDisplayItem(((Item) entity).getItemStack(), null);
    else
      return false;
  }

  @Override
  public void spawn() {
    if (entity != null && entity.isValid() && !entity.isDead())
      return;
    
    if (!Util.hasSpaceForDisplay(getDisplayLocation().getBlock().getType()))
      return;

    ShopDisplayItemSpawnEvent shopDisplayItemSpawnEvent =
        new ShopDisplayItemSpawnEvent(shop, displayItemStack, DisplayData.create(displayItemStack));
    if (Util.fireCancellableEvent(shopDisplayItemSpawnEvent))
      return;
    
    this.entity =
        this.shop.getLocation().world().dropItem(getDisplayLocation(), this.displayItemStack);
    
    Util.debug(
        "Spawned item @ " + this.entity.getLocation() + " with UUID " + this.entity.getUniqueId());
    
    ((Item) this.entity).setItemStack(this.displayItemStack);
    
    Item item = (Item) entity;
    // Set item protect in the armorstand's hand
    if (BaseConfig.displayNameVisible) {
      item.setCustomName(Util.getItemStackName(displayItemStack));
      item.setCustomNameVisible(true);
    }
    item.setPickupDelay(Integer.MAX_VALUE);
    item.setSilent(true);
    item.setPortalCooldown(Integer.MAX_VALUE);
    item.setVelocity(new Vector(0, 0.1, 0));
  }
}
