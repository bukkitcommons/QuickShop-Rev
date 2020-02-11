package org.maxgamer.quickshop.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.ShopViewer;

public class BlockListener implements Listener {
  /*
   * Removes chests when they're destroyed.
   */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    
    if (event.isCancelled()) {
      player.sendMessage(QuickShop.instance().getLocaleManager().getMessage("no-permission", player));
      Util.debug("The action was cancelled by other plugin");
      return;
    }
    
    Block block = event.getBlock();
    BlockState state = block.getState();
    ShopViewer viewer;
    
    if (state instanceof Sign) {
      Sign sign = (Sign) block.getState();
      
      if (BaseConfig.locketteEnable &&
          sign.getLine(0).equals(BaseConfig.lockettePrivateText) ||
          sign.getLine(0).equals(BaseConfig.locketteMoreUsersText))
        return;
      
      viewer = Util.getShopBySign(block);
    } else {
      viewer = Shop.getManager().getLoadedShopAt(block);
    }
    
    viewer
    .nonNull()
    .accept(shop -> {
      boolean isOwner = shop.getModerator().isOwner(player.getUniqueId());
      if (!isOwner &&
          !PermissionManager.instance().has(player, "quickshop.other.destroy")) {
        event.setCancelled(true);
        player.sendMessage(QuickShop.instance().getLocaleManager().getMessage("no-permission", player));
        return;
      }
      
      if (player.getGameMode() == GameMode.CREATIVE) {
        if (player.getInventory().getItemInMainHand().getType() != Material.GOLDEN_AXE) {
          event.setCancelled(true);
          player.sendMessage(QuickShop.instance().getLocaleManager().getMessage("no-creative-break", player, QuickShop.instance().getLocaleManager().getLocalizedName(Material.GOLDEN_AXE.name())));
          return;
        } else {
          player.sendMessage(QuickShop.instance().getLocaleManager().getMessage("break-shop-use-supertool", player));
        }
      }
      
      Shop.getActions().removeAction(player.getUniqueId());
      Shop.getLoader().delete(shop);
      player.sendMessage(QuickShop.instance().getLocaleManager().getMessage("success-removed-shop", player));
    });
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onInventoryMove(InventoryMoveItemEvent event) {
    if (BaseConfig.updateSignOnInvMove) {
      final Inventory inventory = event.getDestination();
      final Location location = inventory.getLocation();

      if (location != null) {
        // Delayed task. Event triggers when item is moved, not when it is received.
        Shop.getManager()
        .getLoadedShopFrom(location)
        .ifPresent(shop -> QuickShop.instance().getSignUpdateWatcher().schedule(shop));
      }
    }
  }

  /*
   * Listens for chest placement, so a doublechest shop can't be created.
   */
  @EventHandler(ignoreCancelled = true)
  public void onPlace(BlockPlaceEvent event) {
    final BlockState state = event.getBlock().getState();
    if (!(state instanceof DoubleChest)) {
      return;
    }

    Util.getSecondHalf(event.getBlock()).ifPresent(chest -> {
      final Player player = event.getPlayer();
      
      if (!PermissionManager.instance().has(player, "quickshop.create.double") &&
          Shop.getManager().hasLoadedShopAt(chest)) {
        event.setCancelled(true);
        player.sendMessage(QuickShop.instance().getLocaleManager().getMessage("no-double-chests", player));
      }
    });
  }
}
