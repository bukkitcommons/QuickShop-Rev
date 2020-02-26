package org.maxgamer.quickshop.listeners;

import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.quickshop.shop.ItemPreviewer;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InvDisplayProtecter implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void invEvent(InventoryInteractEvent e) {
        Inventory inventory = e.getInventory();
        ItemStack[] stacks = inventory.getContents();
        
        for (ItemStack itemStack : stacks) {
            if (!ItemPreviewer.isPreviewItem(itemStack)) {
                continue;
            }
            
            e.setCancelled(true);
            e.setResult(Result.DENY);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void invEvent(InventoryMoveItemEvent e) {
        if (ItemPreviewer.isPreviewItem(e.getItem())) {
            e.setCancelled(true);
        }
    }
    
    @EventHandler
    public void invEvent(InventoryClickEvent e) {
        if (ItemPreviewer.isPreviewItem(e.getCurrentItem())) {
            e.setCancelled(true);
            e.setResult(Result.DENY);
        }
    }
    
    @EventHandler
    public void invEvent(InventoryDragEvent e) {
        if (ItemPreviewer.isPreviewItem(e.getOldCursor())) {
            e.setCancelled(true);
            e.setResult(Result.DENY);
        }
    }
    
    @EventHandler
    public void invEvent(InventoryPickupItemEvent e) {
        Inventory inventory = e.getInventory();
        ItemStack[] stacks = inventory.getContents();
        
        for (ItemStack itemStack : stacks) {
            if (!ItemPreviewer.isPreviewItem(itemStack)) {
                continue;
            }
            
            e.setCancelled(true);
        }
    }
}
