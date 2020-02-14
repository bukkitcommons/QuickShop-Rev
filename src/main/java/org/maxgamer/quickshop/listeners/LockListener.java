package org.maxgamer.quickshop.listeners;

import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.shop.ItemPreviewer;
import org.maxgamer.quickshop.utils.BlockUtils;
import org.maxgamer.quickshop.utils.ShopUtils;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.logger.ShopLogger;

public class LockListener implements Listener {
  @EventHandler(ignoreCancelled = true)
  public void invEvent(InventoryMoveItemEvent e) {
    if (!ItemPreviewer.isPreviewItem(e.getItem())) {
      return;
    }

    e.setCancelled(true);
  }

  @EventHandler
  public void invEvent(InventoryClickEvent e) {
    if (ItemPreviewer.isPreviewItem(e.getCursor())) {
      e.setCancelled(true);
      e.setResult(Event.Result.DENY);
      return;
    }

    if (ItemPreviewer.isPreviewItem(e.getCurrentItem())) {
      e.setCancelled(true);
      e.setResult(Event.Result.DENY);
    }
  }

  @EventHandler
  public void invEvent(InventoryDragEvent e) {
    if (ItemPreviewer.isPreviewItem(e.getCursor())) {
      e.setCancelled(true);
      e.setResult(Event.Result.DENY);
      return;
    }

    if (ItemPreviewer.isPreviewItem(e.getOldCursor())) {
      e.setCancelled(true);
      e.setResult(Event.Result.DENY);
    }
  }

  @EventHandler
  public void invEvent(InventoryPickupItemEvent e) {
    final Inventory inventory = e.getInventory();
    final ItemStack[] stacks = inventory.getContents();

    for (ItemStack itemStack : stacks) {
      if (itemStack == null) {
        continue;
      }

      if (ItemPreviewer.isPreviewItem(itemStack)) {
        e.setCancelled(true);
        return;
      }
    }
  }

  /*
   * Removes chests when they're destroyed.
   */
  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBreak(BlockBreakEvent e) {
    Block b = e.getBlock();

    if (b.getState() instanceof Sign) {
      final Sign sign = (Sign) b.getState();

      if (sign.getLine(0).equals(BaseConfig.lockettePrivateText)
          || sign.getLine(0).equals(BaseConfig.locketteMoreUsersText)) {
        // Ignore break lockette sign
        ShopLogger.instance().info("Skipped a dead-lock shop sign.(Lockette or other sign-lock plugin)");
        return;
      }
    }

    final Player p = e.getPlayer();
    // If the chest was a chest
    if (ShopUtils.canBeShop(b)) {
      Shop.getManager().getLoadedShopFrom(b.getLocation()).ifPresent(shop -> {
        // If they owned it or have bypass perms, they can destroy it
        if (!shop.getOwner().equals(p.getUniqueId())
            && !QuickShopPermissionManager.instance().has(p, "quickshop.other.destroy")) {
          e.setCancelled(true);
          p.sendMessage(Shop.getLocaleManager().get("no-permission", p));
        }
      });
    } else if (BlockUtils.isWallSign(b.getType())) {
      Sign sign = (Sign) b;

      if (sign.getLine(0).equals(BaseConfig.lockettePrivateText)
          || sign.getLine(0).equals(BaseConfig.locketteMoreUsersText)) {
        // Ignore break lockette sign
        Util.debug("Skipped a dead-lock shop sign.(Lockette)");
        return;
      }
      
      Optional<Block> chest = BlockUtils.getSignAttached(b);
      if (!chest.isPresent())
        return;

      Shop.getManager().getLoadedShopAt(b.getLocation()).ifPresent(shop -> {
        // If they're the shop owner or have bypass perms, they can destroy
        // it.
        if (!shop.getOwner().equals(p.getUniqueId())
            && !QuickShopPermissionManager.instance().has(p, "quickshop.other.destroy")) {
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
    Shop.getManager().getLoadedShopFrom(b.getLocation()).ifPresent(shop -> {
      if (!shop.getModerator().isModerator(p.getUniqueId())) {
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
