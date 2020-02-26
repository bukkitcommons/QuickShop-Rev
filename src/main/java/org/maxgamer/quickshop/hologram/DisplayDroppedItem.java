package org.maxgamer.quickshop.hologram;

import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.shop.QuickShopDisplayData;
import org.maxgamer.quickshop.utils.BlockUtils;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.event.ShopDisplayItemSpawnEvent;
import cc.bukkit.shop.hologram.Display;
import cc.bukkit.shop.hologram.DisplayData;
import cc.bukkit.shop.hologram.DisplayType;
import lombok.ToString;

@ToString
public class DisplayDroppedItem extends EntityDisplay implements Display {
    private static final DisplayData DATA = new QuickShopDisplayData(DisplayType.DROPPED_ITEM, new ItemStack(Material.AIR), Collections.emptyMap());
    
    public DisplayDroppedItem(@NotNull ChestShop shop) {
        super(shop, DATA, null); // FIXME
    }
    
    // @Override
    public void spawn() {
        if (entity != null && entity.isValid() && !entity.isDead())
            return;
        
        if (!BlockUtils.hasSpaceForDisplay(location().block().getType()))
            return;
        
        ShopDisplayItemSpawnEvent shopDisplayItemSpawnEvent = new ShopDisplayItemSpawnEvent(shop, displayItemStack, (@NotNull DisplayData) DisplayDataMatcher.create(displayItemStack));
        if (Util.fireCancellableEvent(shopDisplayItemSpawnEvent))
            return;
            
        // this.entity = this.shop.location().world().dropItem(getDisplayLocation(),
        // this.displayItemStack);
        
        Util.debug("Spawned item @ " + this.entity.getLocation() + " with UUID " + this.entity.getUniqueId());
        
        ((Item) this.entity).setItemStack(this.displayItemStack);
        
        Item item = (Item) entity;
        // Set item protect in the armorstand's hand
        if (BaseConfig.displayNameVisible) {
            item.setCustomName(ItemUtils.getItemStackName(displayItemStack));
            item.setCustomNameVisible(true);
        }
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setSilent(true);
        item.setPortalCooldown(Integer.MAX_VALUE);
        item.setVelocity(new Vector(0, 0.1, 0));
    }
}
