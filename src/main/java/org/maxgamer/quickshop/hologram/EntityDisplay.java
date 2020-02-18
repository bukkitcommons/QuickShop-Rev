package org.maxgamer.quickshop.hologram;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.configuration.DisplayConfig;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.hologram.DisplayAttribute;
import cc.bukkit.shop.hologram.DisplayData;
import cc.bukkit.shop.hologram.GenericDisplay;
import cc.bukkit.shop.misc.ShopLocation;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Represents a shop display that have the actual entity.
 * This is a wrapper of the entity instead of its manager.
 */
@Data
@Accessors(fluent = true)
public abstract class EntityDisplay implements GenericDisplay {
  @NotNull
  protected Entity entity;
  @NotNull
  protected ChestShop shop;
  @NotNull
  protected DisplayData data;
  @NotNull
  protected ItemStack displayItemStack;
  @NotNull
  protected ShopLocation location;
  @Setter
  protected boolean pendingRemoval;
  
  public EntityDisplay(@NotNull ChestShop shop, @NotNull DisplayData data, @NotNull Entity entity) {
    this.shop = shop;
    this.data = data;
    this.entity = entity;
    
    displayItemStack = new ItemStack(shop.<ItemStack>stack());
    displayItemStack.setAmount(1);
    displayItemStack = ItemUtils.createGuardItemStack(displayItemStack, shop);
  }
  
  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  public ItemStack sample() {
    return displayItemStack;
  }
  
  @Override
  @NotNull
  public Entity stack() {
    return entity;
  }
  
  @Override
  public void tick() {
    if (!DisplayConfig.displayItems)
      return;
    
    if (!Util.isChunkLoaded(location))
      return;

    if (entity.isValid()) {
      //fixPosition();
    } else {
      Util.debug("Spawning display at: " + location);
      //display.spawn();
    }

    removeDupe();
    
    /*
    Location location = data.scheme();
    boolean fix = data.type() == DisplayType.DROPPED_ITEM ?
        entity.getLocation().distance(location) > 0.6 :
        entity.getLocation().equals(location);
    
    if (fix)
      entity.teleport(location);
    */ // FIXME
  }

  public void removeDupe() {
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
}
