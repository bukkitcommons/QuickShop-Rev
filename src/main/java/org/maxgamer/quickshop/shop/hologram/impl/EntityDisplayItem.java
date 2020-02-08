package org.maxgamer.quickshop.shop.hologram.impl;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.hologram.DisplayAttribute;
import org.maxgamer.quickshop.shop.hologram.DisplayData;
import org.maxgamer.quickshop.shop.hologram.DisplayItem;
import org.maxgamer.quickshop.shop.hologram.DisplayType;
import org.maxgamer.quickshop.utils.Util;

@Getter
@ToString
@Accessors(fluent = true)
public abstract class EntityDisplayItem implements DisplayItem {
  @Nullable
  protected volatile Entity entity;
  @NotNull
  protected Shop shop;
  @NotNull
  protected DisplayData data;
  @NotNull
  protected ItemStack displayItemStack;
  
  protected boolean pendingRemoval;
  
  public EntityDisplayItem(@NotNull Shop shop, @NotNull DisplayData data) {
    this.shop = shop;
    this.data = data;
    
    this.displayItemStack = new ItemStack(shop.getItem());
    this.displayItemStack.setAmount(1);
    this.displayItemStack = DisplayItem.createGuardItemStack(this.displayItemStack, this.shop);
  }
  
  @Override
  public void respawn() {
    remove();
    spawn();
  }

  @Override
  @Nullable
  public Entity getDisplay() {
    return this.entity;
  }

  @Override
  public boolean pendingRemoval() {
    return pendingRemoval = true;
  }

  @Override
  public boolean isPendingRemoval() {
    return pendingRemoval;
  }
  
  @Override
  public void fixPosition() {
    if (this.entity == null)
      return;
    
    if (!this.entity.isValid() || this.entity.isDead())
      respawn();
    else {
      Location location = this.getDisplayLocation();
      boolean fix = data.type() == DisplayType.REALITEM ?
          entity.getLocation().distance(location) > 0.6 :
          entity.getLocation().equals(location);
      
      if (fix)
        entity.teleport(location);
    }
  }

  public void removeDupe() {
    if (entity == null)
      return;
    
    for (Entity entity : entity.getNearbyEntities(1.5, 1.5, 1.5)) {
      switch (entity.getType()) {
        case ARMOR_STAND:
          ArmorStand stand = (ArmorStand) entity;
          
          if (DisplayItem.isDisplayItem(
              stand.getItem(EquipmentSlot.valueOf(data.get(DisplayAttribute.SLOT, "HEAD"))))) {
            
            Util.debug("Removed a duped ArmorStand display entity.");
            entity.remove();
          }
          break;
        case DROPPED_ITEM:
          Item item = (Item) entity;
          DisplayItem.fixesDisplayItem(item);
        default:
          continue;
      }
    }
  }
  
  @Override
  public synchronized boolean isSpawned() {
    return this.entity == null ? false : this.entity.isValid();
  }
  

  @Override
  public void remove() {
    if (this.entity == null)
      return;
    
    this.entity.remove();
    this.entity = null;
    this.displayItemStack = null;
  }
}
