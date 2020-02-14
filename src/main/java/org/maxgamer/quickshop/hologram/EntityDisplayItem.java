package org.maxgamer.quickshop.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.event.ShopDisplayItemDespawnEvent;
import cc.bukkit.shop.hologram.DisplayAttribute;
import cc.bukkit.shop.hologram.DisplayData;
import cc.bukkit.shop.hologram.DisplayItem;
import cc.bukkit.shop.hologram.DisplayType;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@Accessors(fluent = true)
public abstract class EntityDisplayItem implements DisplayItem {
  @Getter
  @Nullable
  protected Entity entity;
  
  @Getter
  @NotNull
  protected ContainerShop shop;
  @Getter
  @NotNull
  protected DisplayData data;
  @NotNull
  protected ItemStack displayItemStack;
  @Nullable
  protected Location location;
  
  protected boolean pendingRemoval;
  
  public EntityDisplayItem(@NotNull ContainerShop shop, @NotNull DisplayData data) {
    this.shop = shop;
    this.data = data;
    
    this.displayItemStack = new ItemStack(shop.getItem());
    this.displayItemStack.setAmount(1);
    this.displayItemStack = ItemUtils.createGuardItemStack(this.displayItemStack, this.shop);
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
    if (entity == null)
      return;
    
    if (!entity.isValid() || this.entity.isDead()) {
      respawn();
    } else {
      Location location = getDisplayLocation();
      boolean fix = data.type() == DisplayType.DROPPED_ITEM ?
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
          
          if (ItemUtils.isDisplayItem(
              stand.getItem(EquipmentSlot.valueOf(data.get(DisplayAttribute.SLOT, "HEAD"))))) {
            
            Util.debug("Removed a duped ArmorStand display entity.");
            entity.remove();
          }
          break;
        case DROPPED_ITEM:
          Item item = (Item) entity;
          ItemUtils.fixesDisplayItem(item);
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
    
    ShopDisplayItemDespawnEvent shopDisplayItemDespawnEvent =
        new ShopDisplayItemDespawnEvent(this.shop, displayItemStack, data);
    Bukkit.getPluginManager().callEvent(shopDisplayItemDespawnEvent);
  }
}
