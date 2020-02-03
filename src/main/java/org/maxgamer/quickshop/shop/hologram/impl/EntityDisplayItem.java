package org.maxgamer.quickshop.shop.hologram.impl;

import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.hologram.DisplayItem;

@ToString
public abstract class EntityDisplayItem implements DisplayItem {
  @Nullable
  protected volatile Entity entity;
  @NotNull
  protected  Shop shop;
  
  @Nullable
  protected ItemStack guardedIstack;
  protected ItemStack originalItemStack;
  
  protected boolean pendingRemoval;
  
  public EntityDisplayItem(@NotNull Shop shop) {
    this.shop = shop;
    this.originalItemStack = new ItemStack(shop.getItem());
    this.originalItemStack.setAmount(1);
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
    else if (this.entity.getLocation().equals(getDisplayLocation())) {
      Location location = this.getDisplayLocation();
      this.entity.teleport(location);
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
    this.guardedIstack = null;
  }
}
