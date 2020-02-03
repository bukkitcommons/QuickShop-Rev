package org.maxgamer.quickshop.listeners;

import lombok.AllArgsConstructor;
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
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.shop.Info;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.ShopAction;
import org.maxgamer.quickshop.utils.MsgUtil;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

@AllArgsConstructor
public class BlockListener implements Listener {

  @NotNull
  private final QuickShop plugin;

  /**
   * Gets the shop a sign is attached to
   *
   * @param loc The location of the sign
   * @return The shop
   */
  private ShopViewer getShopNextTo(@NotNull Location loc) {
    final Block b = Util.getAttached(loc.getBlock());
    // Util.getAttached(b)
    if (b == null) {
      return null;
    }

    return plugin.getShopManager().getShop(b.getLocation());
  }

  /*
   * Removes chests when they're destroyed.
   */
  @EventHandler(priority = EventPriority.LOW)
  public void onBreak(BlockBreakEvent e) {

    final Block b = e.getBlock();

    if (b.getState() instanceof Sign) {
      Sign sign = (Sign) b.getState();
      if (BaseConfig.locketteEnable && sign.getLine(0).equals(BaseConfig.lockettePrivateText)
          || sign.getLine(0).equals(BaseConfig.locketteMoreUsersText)) {
        // Ignore break lockette sign
        plugin.getLogger()
            .info("Skipped a dead-lock shop sign.(Lockette or other sign-lock plugin)");
        return;
      }
    }

    final Player p = e.getPlayer();
    // If the shop was a chest
    if (Util.canBeShop(b)) {
      final ShopViewer shop = plugin.getShopManager().getShop(b.getLocation());
      if (shop.get() == null) {
        return;
      }
      // If they're either survival or the owner, they can break it
      if (p.getGameMode() == GameMode.CREATIVE && !p.getUniqueId().equals(shop.get().getOwner())) {
        // Check SuperTool
        if (p.getInventory().getItemInMainHand().getType() == Material.GOLDEN_AXE) {
          p.sendMessage(MsgUtil.getMessage("break-shop-use-supertool", p));
          return;
        }
        e.setCancelled(true);
        p.sendMessage(MsgUtil.getMessage("no-creative-break", p,
            MsgUtil.getLocalizedName(Material.GOLDEN_AXE.name())));
        return;
      }

      if (e.isCancelled()) {
        p.sendMessage(MsgUtil.getMessage("no-permission", p));
        Util.debugLog("The action was cancelled by other plugin");
        return;
      }

      if (!shop.get().getModerator().isOwner(p.getUniqueId())
          && !QuickShop.getPermissionManager().hasPermission(p, "quickshop.other.destroy")) {
        e.setCancelled(true);
        p.sendMessage(MsgUtil.getMessage("no-permission", p));
        return;
      }
      // Cancel their current menu... Doesnt cancel other's menu's.
      final Info action = plugin.getShopManager().getActions().get(p.getUniqueId());

      if (action != null) {
        action.setAction(ShopAction.CANCELLED);
      }

      shop.get().onUnload();
      shop.get().delete();
      p.sendMessage(MsgUtil.getMessage("success-removed-shop", p));
    } else if (Util.isWallSign(b.getType())) {
      if (b instanceof Sign) {
        Sign sign = (Sign) b;
        if (sign.getLine(0).equals(BaseConfig.lockettePrivateText)
            || sign.getLine(0).equals(BaseConfig.locketteMoreUsersText)) {
          // Ignore break lockette sign
          return;
        }
      }

      ShopViewer viewer = getShopNextTo(b.getLocation());

      viewer.nonNull()
        .accept(shop -> {
          // If they're in creative and not the owner, don't let them
          // (accidents happen)
          if (p.getGameMode() == GameMode.CREATIVE && !p.getUniqueId().equals(shop.getOwner())) {
            // Check SuperTool
            if (p.getInventory().getItemInMainHand().getType() == Material.GOLDEN_AXE) {
              p.sendMessage(MsgUtil.getMessage("break-shop-use-supertool", p));
              shop.delete();
              return;
            }
            e.setCancelled(true);
            p.sendMessage(MsgUtil.getMessage("no-creative-break", p,
                MsgUtil.getLocalizedName(Material.GOLDEN_AXE.name())));
          }
  
          Util.debugLog("Cannot break the sign.");
          e.setCancelled(true);
        });
    }
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
    final ShopViewer shop = plugin.getShopManager().getShopIncludeAttached(location);
    if (shop.get() != null) {
      plugin.getSignUpdateWatcher().scheduleSignUpdate(shop.get());
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
    final Block chest = Util.getSecondHalf(b);

    if (chest != null && plugin.getShopManager().getShop(chest.getLocation()) != null
        && !QuickShop.getPermissionManager().hasPermission(p, "quickshop.create.double")) {
      e.setCancelled(true);
      p.sendMessage(MsgUtil.getMessage("no-double-chests", p));
    }
  }
}