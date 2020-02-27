package org.maxgamer.quickshop.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.scheduler.SlackServices;
import org.maxgamer.quickshop.utils.BlockUtils;
import org.maxgamer.quickshop.utils.ShopUtils;

import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.ShopViewer;
import lombok.val;

public class BlockListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        val player = event.getPlayer();
        
        val block = event.getBlock();
        val state = block.getState();
        ShopViewer viewer;
        
        if (state instanceof Sign) {
            val sign = (Sign) block.getState();
            
            if (BaseConfig.locketteEnable && sign.getLine(0).equals(BaseConfig.lockettePrivateText) || sign.getLine(0).equals(BaseConfig.locketteMoreUsersText))
                return;
            
            viewer = ShopUtils.getShopBySign(block);
        } else
            viewer = Shop.getManager().getLoadedShopAt(block);
        
        viewer.nonNull().accept((ChestShop shop) -> {
            val isOwner = shop.moderator().isOwner(player.getUniqueId());
            if (!isOwner && !QuickShopPermissionManager.instance().has(player, "quickshop.other.destroy")) {
                event.setCancelled(true);
                player.sendMessage(Shop.getLocaleManager().get("no-permission", player));
                return;
            }
            
            if (player.getGameMode() == GameMode.CREATIVE) {
                Material tool = Material.getMaterial("GOLDEN_AXE");
                tool = tool == null ? Material.getMaterial("GOLD_AXE") : tool;
                if (player.getInventory().getItemInMainHand().getType() != tool) {
                    event.setCancelled(true);
                    player.sendMessage(Shop.getLocaleManager().get("no-creative-break", player, Shop.getLocaleManager().get(tool)));
                    return;
                } else
                    player.sendMessage(Shop.getLocaleManager().get("break-shop-use-supertool", player));
            }
            
            Shop.getActions().removeAction(player.getUniqueId());
            Shop.getLoader().delete(shop);
            player.sendMessage(Shop.getLocaleManager().get("success-removed-shop", player));
        });
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (BaseConfig.updateSignOnInvMove) {
            val inventory = event.getDestination();
            val location = inventory.getLocation();
            
            if (location != null)
                // Delayed task. Event triggers when item is moved, not when it is received.
                Shop.getManager().getLoadedShopFrom(location).ifPresent((ChestShop shop) -> {
                    SlackServices.schedule(shop, s -> {
                        s.setSignText();
                        Shop.getManager().save(s);
                    });
                });
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        BlockUtils.getSecondHalf(event.getBlock()).ifPresent(chest -> {
            val player = event.getPlayer();
            
            if (!QuickShopPermissionManager.instance().has(player, "quickshop.create.double") && Shop.getManager().hasLoadedShopAt(chest)) {
                event.setCancelled(true);
                player.sendMessage(Shop.getLocaleManager().get("no-double-chests", player));
            }
        });
    }
}
