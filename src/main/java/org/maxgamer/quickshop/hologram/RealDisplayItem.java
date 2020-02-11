package org.maxgamer.quickshop.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.event.ShopDisplayItemSpawnEvent;
import cc.bukkit.shop.hologram.DisplayData;
import cc.bukkit.shop.hologram.DisplayItem;
import cc.bukkit.shop.hologram.DisplayType;
import lombok.ToString;

@ToString
public class RealDisplayItem extends EntityDisplayItem implements DisplayItem {
  private static final DisplayData DATA = new DisplayData(DisplayType.DROPPED_ITEM);
  
  public RealDisplayItem(@NotNull ContainerShop shop) {
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
      return Util.isDisplayItem(((Item) entity).getItemStack(), null);
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
        new ShopDisplayItemSpawnEvent(shop, displayItemStack, DisplayDataMatcher.create(displayItemStack));
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
