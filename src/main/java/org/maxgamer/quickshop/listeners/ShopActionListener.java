package org.maxgamer.quickshop.listeners;

import java.util.Arrays;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
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
public class ShopActionListener implements Listener {

  /*
   * Pre-handling shop actions
   */
  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onClick(PlayerInteractEvent e) {
    @Nullable Block block = e.getClickedBlock();
    if (block == null)
      return;
    
    ShopViewer viewer = ShopManager.instance().getLoadedShopFrom(block);
    /*
     * Effect handling
     */
    viewer
      .nonNull()
      .filter(shop -> BaseConfig.clickSound)
      .accept(shop -> {
        e.getPlayer().playSound(
            shop.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 80.f, 1.0f);
      });
    
    /*
     * Control handling
     */
    if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
      viewer
        .nonNull()
        .filter(shop ->
          !BaseConfig.sneakToControl || e.getPlayer().isSneaking())
        .filter(shop ->
          shop.getOwner().equals(e.getPlayer().getUniqueId()) || e.getPlayer().isOp())
        
        .accept(shop -> {
          MsgUtil.sendControlPanelInfo(e.getPlayer(), shop);
          //shop.setSignText();
        });
      
      return;
    }

    Player player = e.getPlayer();
    ItemStack item = e.getItem();
    
    /*
     * Trade handling
     */
    viewer
      .nonNull()
      .filter(shop ->
        !BaseConfig.sneakToTrade || e.getPlayer().isSneaking())
      .filter(shop ->
        QuickShop.getPermissionManager().hasPermission(player, "quickshop.use"))    
      
      .accept(shop -> {
        shop.onClick();
        MsgUtil.sendShopInfo(player, shop);
        //shop.setSignText();
        
        double price = shop.getPrice();
        double money = QuickShop.instance().getEconomy().getBalance(player.getUniqueId());

        if (shop.is(ShopType.SELLING)) {
          int itemAmount = Math.min(Util.countSpace(player.getInventory(), shop.getItem()),
              (int) Math.floor(money / price));

          if (!shop.isUnlimited()) {
            itemAmount = Math.min(itemAmount, shop.getRemainingStock());
          }

          if (itemAmount < 0)
            itemAmount = 0;

          player.sendMessage(MsgUtil.getMessage("how-many-buy", player, "" + itemAmount));
        } else {
          double ownerBalance = QuickShop.instance().getEconomy().getBalance(shop.getOwner());
          int items = Util.countItems(player.getInventory(), shop.getItem());
          int ownerCanAfford = (int) (ownerBalance / price);

          if (!shop.isUnlimited()) {
            items = Math.min(items, shop.getRemainingSpace());
            items = Math.min(items, ownerCanAfford);
          } else if (BaseConfig.payUnlimitedShopOwners) {
            items = Math.min(items, ownerCanAfford);
          }

          if (items < 0)
            items = 0;

          player.sendMessage(MsgUtil.getMessage("how-many-sell", player, "" + items));
        }
        
        ShopActionManager.instance().setAction(player.getUniqueId(), new ShopSnapshot(shop));
      });
    
    /*
     * Creation handling
     */
    viewer
      .nonNull()
      .filter(shop ->
        e.useInteractedBlock() == Result.ALLOW && item != null && item.getType() != Material.AIR)
      .filter(shop ->
        player.getGameMode() != GameMode.CREATIVE &&
        QuickShop.getPermissionManager().hasPermission(player, "quickshop.create.sell"))
      
      .filter(shop -> !BaseConfig.sneakToCreat || player.isSneaking())
      .filter(shop -> ShopManager.canBuildShop(player, block))
      
      .accept(shop -> {
        if (Util.getSecondHalf(block).isPresent() &&
            !QuickShop.getPermissionManager().hasPermission(player, "quickshop.create.double")) {
          player.sendMessage(MsgUtil.getMessage("no-double-chests", player));
          return;
        }

        if (Util.isBlacklisted(item) &&
            !QuickShop.getPermissionManager().hasPermission(player, "quickshop.bypass." + item.getType().name())) {
          player.sendMessage(MsgUtil.getMessage("blacklisted-item", player));
          return;
        }

        if (block.getType() == Material.ENDER_CHEST &&
            !QuickShop.getPermissionManager().hasPermission(player, "quickshop.create.enderchest")) {
          return;
        }

        if (Util.isWallSign(block.getType()) &&
            !Arrays.stream(((Sign) block.getState()).getLines()).allMatch(String::isEmpty))
          return;
        
        // Finds out where the sign should be placed for the shop
        Block last = null;
        final Location from = player.getLocation().clone();

        from.setY(block.getY());
        from.setPitch(0);
        final BlockIterator bIt = new BlockIterator(from, 0, 7);

        while (bIt.hasNext()) {
          final Block n = bIt.next();

          if (n.equals(block)) {
            break;
          }

          last = n;
        }
        // Send creation menu.
        final ShopCreator info = new ShopCreator(block.getLocation(), e.getItem(), last);

        ShopActionManager.instance().getActions().put(player.getUniqueId(), info);
        player.sendMessage(MsgUtil.getMessage("how-much-to-trade-for", player,
            Util.getItemStackName(Objects.requireNonNull(e.getItem()))));
      });
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

    ShopManager.instance().getLoadedShopFrom(location).ifPresent(Shop::setSignText);
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

    if (Util.getShopBySign(block).isPresent()) {
      e.setCancelled(true);
      Util.debugLog("Disallow " + e.getPlayer().getName() + " dye the shop sign.");
    }
  }
}
