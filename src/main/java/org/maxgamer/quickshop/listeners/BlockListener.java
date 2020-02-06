package org.maxgamer.quickshop.listeners;

import java.util.Optional;
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
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.shop.ShopActionManager;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

public class BlockListener implements Listener {
  /*
   * Removes chests when they're destroyed.
   */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    
    if (event.isCancelled()) {
      player.sendMessage(MsgUtil.getMessage("no-permission", player));
      Util.debugLog("The action was cancelled by other plugin");
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
      viewer = ShopManager.instance().getShopAt(block);
    }
    
    viewer.nonNull()
    .accept(shop -> {
      boolean isOwner = shop.getModerator().isOwner(player.getUniqueId());
      if (!isOwner &&
          !QuickShop.getPermissionManager().hasPermission(player, "quickshop.other.destroy")) {
        event.setCancelled(true);
        player.sendMessage(MsgUtil.getMessage("no-permission", player));
        return;
      }
      
      if (player.getGameMode() == GameMode.CREATIVE) {
        if (player.getInventory().getItemInMainHand().getType() != Material.GOLDEN_AXE) {
          event.setCancelled(true);
          player.sendMessage(MsgUtil.getMessage("no-creative-break", player, MsgUtil.getLocalizedName(Material.GOLDEN_AXE.name())));
          return;
        } else {
          player.sendMessage(MsgUtil.getMessage("break-shop-use-supertool", player));
        }
      }
      
      ShopActionManager.instance().getActions().remove(player.getUniqueId());
      ShopManager.instance().delete(shop);
      player.sendMessage(MsgUtil.getMessage("success-removed-shop", player));
    });
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onInventoryMove(InventoryMoveItemEvent event) {
    if (!BaseConfig.updateSignOnInvMove) {
      return;
    }

    final Inventory inventory = event.getDestination();
    final Location location = inventory.getLocation();

    if (location == null) {
      return;
    }

    // Delayed task. Event triggers when item is moved, not when it is received.
    final ShopViewer shop = ShopManager.instance().getShopFrom(location);
    if (shop.get() != null) {
      QuickShop.instance().getSignUpdateWatcher().scheduleSignUpdate(shop.get());
    }
  }

  /*
   * Listens for chest placement, so a doublechest shop can't be created.
   */
  @EventHandler(ignoreCancelled = true)
  public void onPlace(BlockPlaceEvent e) {
    final BlockState bs = e.getBlock().getState();

    if (!(bs instanceof DoubleChest)) {
      return;
    }

    final Block b = e.getBlock();
    final Player p = e.getPlayer();
    final Optional<Location> chest = Util.getSecondHalf(b);

    if (chest.isPresent() && ShopManager.instance().getShopAt(chest.get()) != null
        && !QuickShop.getPermissionManager().hasPermission(p, "quickshop.create.double")) {
      e.setCancelled(true);
      p.sendMessage(MsgUtil.getMessage("no-double-chests", p));
    }
  }
}
