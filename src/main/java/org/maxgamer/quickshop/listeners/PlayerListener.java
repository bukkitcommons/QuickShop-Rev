/*
 * This file is a part of project QuickShop, the name is PlayerListener.java Copyright (C) Ghost_chu
 * <https://github.com/Ghost-chu> Copyright (C) Bukkit Commons Studio and contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.maxgamer.quickshop.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.economy.Economy;
import org.maxgamer.quickshop.shop.ShopActionManager;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopType;
import org.maxgamer.quickshop.shop.api.data.ShopAction;
import org.maxgamer.quickshop.shop.api.data.ShopCreator;
import org.maxgamer.quickshop.shop.api.data.ShopData;
import org.maxgamer.quickshop.shop.api.data.ShopSnapshot;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

// import com.griefcraft.lwc.LWC;
// import com.griefcraft.lwc.LWCPlugin;
@AllArgsConstructor
public class PlayerListener implements Listener {

  @NotNull
  private final QuickShop plugin;

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onClick(PlayerInteractEvent e) {

    if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK) && e.getClickedBlock() != null) {
      if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
          && Util.isWallSign(e.getClickedBlock().getType())) {
        final Block block;

        if (Util.isWallSign(e.getClickedBlock().getType())) {
          block = Util.getSignAttached(e.getClickedBlock());
        } else {
          block = e.getClickedBlock();
        }
        
        ShopViewer optional = ShopManager.instance().getShopAt(block.getLocation());

        if (optional.isPresent()
            && (optional.get()
                .getOwner().equals(e.getPlayer().getUniqueId()) || e.getPlayer().isOp())) {
          if (BaseConfig.sneakToControl
              && !e.getPlayer().isSneaking()) {
            return;
          }

          if (BaseConfig.clickSound) {
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_DISPENSER_FAIL, 80.f,
                1.0f);
          }

          MsgUtil.sendControlPanelInfo(e.getPlayer(), optional.get());
          optional.get().setSignText();
        }
      }

      return;
    }

    final Block b = e.getClickedBlock();

    if (b == null) {
      return;
    }

    if (!Util.canBeShop(b) && !Util.isWallSign(b.getType())) {
      return;
    }

    final Player p = e.getPlayer();
    final Location loc = b.getLocation();
    final ItemStack item = e.getItem();
    // Get the shop
    ShopViewer shop = ShopManager.instance().getShopAt(loc);
    // If that wasn't a shop, search nearby shops
    if (!shop.isPresent()) {
      final Block attached;

      if (Util.isWallSign(b.getType())) {
        attached = Util.getSignAttached(b);

        if (attached != null) {
          shop = ShopManager.instance().getShopAt(attached.getLocation());
        }
      } else if (Util.isDoubleChest(b)) {
        attached = Util.getSecondHalf(b);

        if (attached != null) {
          ShopViewer secondHalfShop = ShopManager.instance().getShopAt(attached.getLocation());
          if (secondHalfShop.isPresent() && !p.getUniqueId().equals(secondHalfShop.get().getOwner())) {
            // If player not the owner of the shop, make him select the second half of the
            // shop
            // Otherwise owner will be able to create new double chest shop
            shop = secondHalfShop;
          }
        }
      }
    }
    // Purchase handling
    if (shop.isPresent() && QuickShop.getPermissionManager().hasPermission(p, "quickshop.use")) {
      if (BaseConfig.sneakToTrade && !p.isSneaking()) {
        return;
      }

      shop.get().onClick();

      if (BaseConfig.clickSound) {
        e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_DISPENSER_FAIL, 80.f,
            1.0f);
      }
      // Text menu
      MsgUtil.sendShopInfo(p, shop.get());
      shop.get().setSignText();

      final Economy eco = plugin.getEconomy();
      final double price = shop.get().getPrice();
      final double money = plugin.getEconomy().getBalance(p.getUniqueId());

      if (shop.get().getShopType() == ShopType.SELLING) {
        int itemAmount = Math.min(Util.countSpace(p.getInventory(), shop.get().getItem()),
            (int) Math.floor(money / price));

        if (!shop.get().isUnlimited()) {
          itemAmount = Math.min(itemAmount, shop.get().getRemainingStock());
        }

        if (itemAmount < 0) {
          itemAmount = 0;
        }

        p.sendMessage(MsgUtil.getMessage("how-many-buy", p, "" + itemAmount));
      } else {
        final double ownerBalance = eco.getBalance(shop.get().getOwner());
        int items = Util.countItems(p.getInventory(), shop.get().getItem());
        final int ownerCanAfford = (int) (ownerBalance / shop.get().getPrice());

        if (!shop.get().isUnlimited()) {
          // Amount check player amount and shop empty slot
          items = Math.min(items, shop.get().getRemainingSpace());
          // Amount check player selling item total cost and the shop owner's balance
          items = Math.min(items, ownerCanAfford);
        } else if (BaseConfig.payUnlimitedShopOwners) {
          // even if the shop is unlimited, the config option pay-unlimited-shop-owners is set to
          // true,
          // the unlimited shop owner should have enough money.
          items = Math.min(items, ownerCanAfford);
        }

        if (items < 0) {
          items = 0;
        }

        p.sendMessage(MsgUtil.getMessage("how-many-sell", p, "" + items));
      }
      // Add the new action
      Map<UUID, ShopData> actions = ShopActionManager.instance().getActions();
      ShopSnapshot info = new ShopSnapshot(shop.get());
      actions.put(p.getUniqueId(), info);
    }
    // Handles creating shops
    else if (e.useInteractedBlock() == Result.ALLOW && shop.isPresent() && item != null
        && item.getType() != Material.AIR
        && QuickShop.getPermissionManager().hasPermission(p, "quickshop.create.sell")
        && p.getGameMode() != GameMode.CREATIVE) {
      if (e.useInteractedBlock() == Result.DENY
          || BaseConfig.sneakToCreat && !p.isSneaking()
          || !ShopManager.instance().canBuildShop(p, b, e.getBlockFace())) {
        // As of the new checking system, most plugins will tell the
        // player why they can't create a shop there.
        // So telling them a message would cause spam etc.
        return;
      }

      if (Util.getSecondHalf(b) != null
          && !QuickShop.getPermissionManager().hasPermission(p, "quickshop.create.double")) {
        p.sendMessage(MsgUtil.getMessage("no-double-chests", p));
        return;
      }

      if (Util.isBlacklisted(item) && !QuickShop.getPermissionManager().hasPermission(p,
          "quickshop.bypass." + item.getType().name())) {
        p.sendMessage(MsgUtil.getMessage("blacklisted-item", p));
        return;
      }

      if (b.getType() == Material.ENDER_CHEST
          && !QuickShop.getPermissionManager().hasPermission(p, "quickshop.create.enderchest")) {
        return;
      }
      /*
       * if (!Util.canBeShop(b)) { Util.debugLog("Can be shop check failed."); return; } Already
       * checked above
       */

      if (Util.isWallSign(b.getType())) {
        Util.debugLog("WallSign check failed.");
        return;
      }
      // Finds out where the sign should be placed for the shop
      Block last = null;
      final Location from = p.getLocation().clone();

      from.setY(b.getY());
      from.setPitch(0);
      final BlockIterator bIt = new BlockIterator(from, 0, 7);

      while (bIt.hasNext()) {
        final Block n = bIt.next();

        if (n.equals(b)) {
          break;
        }

        last = n;
      }
      // Send creation menu.
      final ShopCreator info = new ShopCreator(b.getLocation(), e.getItem(), last);

      ShopActionManager.instance().getActions().put(p.getUniqueId(), info);
      p.sendMessage(MsgUtil.getMessage("how-much-to-trade-for", p,
          Util.getItemStackName(Objects.requireNonNull(e.getItem()))));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryClose(InventoryCloseEvent e) {

    @Nullable
    Inventory inventory = e.getInventory(); // Possibly wrong tag
    @Nullable
    Location location = null;

    try {
      // This will cause NPE when the internal getLocation method
      // itself NPE, which should be a server issue.
      location = inventory.getLocation();
    } catch (NullPointerException npe) {
      return; // ignored as workaround, GH-303
    }

    if (location == null)
      return;

    ShopManager.instance().getShopFrom(location).ifPresent(Shop::setSignText);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {

    // Notify the player any messages they were sent
    if (BaseConfig.autoFetchShopMessages) {
      MsgUtil.flushMessagesFor(event.getPlayer());
    }
  }

  /*
   * Waits for a player to move too far from a shop, then cancels the menu.
   */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onMove(PlayerMoveEvent e) {

    final ShopData info = ShopActionManager.instance().getActions().get(e.getPlayer().getUniqueId());

    if (info == null) {
      return;
    }

    final Player p = e.getPlayer();
    final Location loc1 = info.location();
    final Location loc2 = p.getLocation();

    if (loc1.getWorld() != loc2.getWorld() || loc1.distanceSquared(loc2) > 25) {
      if (info.action() == ShopAction.CREATE) {
        p.sendMessage(MsgUtil.getMessage("shop-creation-cancelled", p));
        Util.debugLog(p.getName() + " too far with the shop location.");
      } else if (info.action() == ShopAction.TRADE) {
        p.sendMessage(MsgUtil.getMessage("shop-purchase-cancelled", p));
        Util.debugLog(p.getName() + " too far with the shop location.");
      }
      ShopActionManager.instance().getActions().remove(p.getUniqueId());
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent e) {

    // Remove them from the menu
    ShopActionManager.instance().getActions().remove(e.getPlayer().getUniqueId());
  }

  @EventHandler(ignoreCancelled = true)
  public void onTeleport(PlayerTeleportEvent e) {

    PlayerMoveEvent me = new PlayerMoveEvent(e.getPlayer(), e.getFrom(), e.getTo());
    onMove(me);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onDyeing(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getItem() == null
        || !Util.isDyes(e.getItem().getType())) {
      return;
    }

    final Block block = e.getClickedBlock();

    if (block == null || !Util.isWallSign(block.getType())) {
      return;
    }

    final Block attachedBlock = Util.getSignAttached(block);

    if (attachedBlock == null
        || ShopManager.instance().getShopFrom(attachedBlock.getLocation()) == null) {
      return;
    }

    e.setCancelled(true);
    Util.debugLog("Disallow " + e.getPlayer().getName() + " dye the shop sign.");
  }
}
