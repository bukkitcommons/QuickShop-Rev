package org.maxgamer.quickshop.listeners;

import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.utils.BlockUtils;
import org.maxgamer.quickshop.utils.ShopUtils;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.BasicShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.logger.ShopLogger;

public class LockListener implements Listener {
    /*
     * Removes chests when they're destroyed.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        
        if (b.getState() instanceof Sign) {
            final Sign sign = (Sign) b.getState();
            
            if (sign.getLine(0).equals(BaseConfig.lockettePrivateText) || sign.getLine(0).equals(BaseConfig.locketteMoreUsersText)) {
                // Ignore break lockette sign
                ShopLogger.instance().info("Skipped a dead-lock shop sign.(Lockette or other sign-lock plugin)");
                return;
            }
        }
        
        final Player p = e.getPlayer();
        // If the chest was a chest
        if (ShopUtils.canBeShop(b)) {
            Shop.getManager().getLoadedShopFrom(b.getLocation()).ifPresent((BasicShop shop) -> {
                // If they owned it or have bypass perms, they can destroy it
                if (!shop.getOwner().equals(p.getUniqueId()) && !QuickShopPermissionManager.instance().has(p, "quickshop.other.destroy")) {
                    e.setCancelled(true);
                    p.sendMessage(Shop.getLocaleManager().get("no-permission", p));
                }
            });
        } else
            if (BlockUtils.isWallSign(b.getType())) {
                Sign sign = (Sign) b.getState();
                
                if (sign.getLine(0).equals(BaseConfig.lockettePrivateText) || sign.getLine(0).equals(BaseConfig.locketteMoreUsersText)) {
                    // Ignore break lockette sign
                    Util.trace("Skipped a dead-lock shop sign.(Lockette)");
                    return;
                }
                
                Optional<Block> chest = BlockUtils.getSignAttached(b);
                if (!chest.isPresent())
                    return;
                
                Shop.getManager().getLoadedShopAt(b.getLocation()).ifPresent((BasicShop shop) -> {
                    // If they're the shop owner or have bypass perms, they can destroy
                    // it.
                    if (!shop.getOwner().equals(p.getUniqueId()) && !QuickShopPermissionManager.instance().has(p, "quickshop.other.destroy")) {
                        e.setCancelled(true);
                        p.sendMessage(Shop.getLocaleManager().get("no-permission", p));
                    }
                });
            }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onClick(PlayerInteractEvent e) {
        
        final Block b = e.getClickedBlock();
        
        if (b == null) {
            return;
        }
        
        if (!ShopUtils.canBeShop(b)) {
            return;
        }
        
        final Player p = e.getPlayer();
        
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return; // Didn't right click it, we dont care.
        }
        
        // Make sure they're not using the non-shop half of a double chest.
        Shop.getManager().getLoadedShopFrom(b.getLocation()).ifPresent((BasicShop shop) -> {
            if (!shop.moderator().isModerator(p.getUniqueId())) {
                if (QuickShopPermissionManager.instance().has(p, "quickshop.other.open")) {
                    p.sendMessage(Shop.getLocaleManager().get("bypassing-lock", p));
                    return;
                }
                p.sendMessage(Shop.getLocaleManager().get("that-is-locked", p));
                e.setCancelled(true);
            }
        });
    }
    
    /*
     * Handles hopper placement
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        
        final Block b = e.getBlock();
        
        if (b.getType() != Material.HOPPER) {
            return;
        }
        
        final Player p = e.getPlayer();
        
        if (!Util.isOtherShopWithinHopperReach(b, p)) {
            return;
        }
        
        if (QuickShopPermissionManager.instance().has(p, "quickshop.other.open")) {
            p.sendMessage(Shop.getLocaleManager().get("bypassing-lock", p));
            return;
        }
        
        p.sendMessage(Shop.getLocaleManager().get("that-is-locked", p));
        e.setCancelled(true);
    }
}
