package org.maxgamer.quickshop.listeners;

import java.util.Arrays;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.bukkit.ChatColor;
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
import org.maxgamer.quickshop.shop.api.data.ShopLocation;
import org.maxgamer.quickshop.shop.api.data.ShopSnapshot;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

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
    Util.debug(ChatColor.YELLOW + "> Handling interact");
    
    ShopViewer viewer = ShopManager.instance().getLoadedShopFrom(block);
    /*
     * Effect handling
     */
    viewer
      .nonNull()
      .filter(shop -> BaseConfig.clickSound)
      .accept(shop -> {
        e.getPlayer().playSound(
            shop.getLocation().bukkit(), Sound.BLOCK_DISPENSER_FAIL, 80.f, 1.0f);
      });
    
    /*
     * Control handling
     */
    if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
      Util.debug("STAGE -1");
      
      viewer
        .nonNull()
        .filter(shop ->
          !BaseConfig.sneakToControl || e.getPlayer().isSneaking())
        .filter(shop ->
          shop.getOwner().equals(e.getPlayer().getUniqueId()) || e.getPlayer().isOp())
        
        .accept(shop -> {
          Util.debug(ChatColor.GREEN + "Handling control");
          
          MsgUtil.sendControlPanelInfo(e.getPlayer(), shop);
          shop.setSignText();
        });
      
      return;
    }

    Util.debug("STAGE 0");
    Player player = e.getPlayer();
    
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
        Util.debug(ChatColor.GREEN + "Handling trade");
        
        shop.onClick();
        MsgUtil.sendShopInfo(player, shop);
        shop.setSignText();
        
        double price = shop.getPrice();
        double money = QuickShop.instance().getEconomy().getBalance(player.getUniqueId());

        if (shop.is(ShopType.SELLING)) {
          // Consider player inv space, money afforable
          int afforable = Math.min(Util.countSpace(player.getInventory(), shop.getItem()),
              (int) Math.floor(money / price));

          // Consider shop remaining stock
          if (!shop.isUnlimited()) {
            afforable = Math.min(afforable, shop.getRemainingStock());
          }

          if (afforable < 0)
            afforable = 0;

          player.sendMessage(MsgUtil.getMessage("how-many-buy", player, "" + afforable));
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
    
    Util.debug("STAGE 1");
    ItemStack item = e.getItem();
    
    /*
     * Creation handling
     */
    viewer
      .reset() // Reset but do not check null
      .filter(shop ->
        e.useInteractedBlock() == Result.ALLOW)
      
      .filter(shop ->
        player.getGameMode() != GameMode.CREATIVE &&
        QuickShop.getPermissionManager().hasPermission(player, "quickshop.create.sell"))
      
      .filter(shop -> !BaseConfig.sneakToCreat || player.isSneaking())
      .filter(shop -> ShopManager.canBuildShop(player, block))
      
      .accept(shop -> {
        Util.debug(ChatColor.GREEN + "Handling creation.");
        
        if (!Util.canBeShopIgnoreBlocklist(block.getState())) {
          Util.debug("Block cannot be shop.");
          return;
          /*
          if (!QuickShop.getPermissionManager().hasPermission(player, "quickshop.bypass." + item.getType().name())) {
            player.sendMessage(MsgUtil.getMessage("blacklisted-item", player));
            return;
          }
          */
        }
        
        if (item == null || item.getType() == Material.AIR) {
          player.sendMessage(MsgUtil.getMessage("no-anythings-in-your-hand", player));
          return;
        }
        
        if (Util.getSecondHalf(block).isPresent() &&
            !QuickShop.getPermissionManager().hasPermission(player, "quickshop.create.double")) {
          player.sendMessage(MsgUtil.getMessage("no-double-chests", player));
          return;
        }

        if (block.getType() == Material.ENDER_CHEST &&
            !QuickShop.getPermissionManager().hasPermission(player, "quickshop.create.enderchest")) {
          return;
        }
        
        // Finds out where the sign should be placed for the shop
        Block expectedSign = null;
        final Location from = player.getLocation();

        from.setY(block.getY());
        from.setPitch(0);
        final BlockIterator bIt = new BlockIterator(from, 0, 7);

        while (bIt.hasNext()) {
          final Block n = bIt.next();

          if (n.equals(block)) {
            break;
          }

          expectedSign = n;
        }
        
        if (expectedSign != null && Util.isWallSign(expectedSign.getType()) &&
            !Arrays.stream(((Sign) expectedSign.getState()).getLines()).allMatch(String::isEmpty))
          return;
        
        // Send creation menu.
        ShopCreator info = ShopCreator.create(ShopLocation.of(block.getLocation()), expectedSign, item);

        ShopActionManager.instance().getActions().put(player.getUniqueId(), info);
        player.sendMessage(MsgUtil.getMessage("how-much-to-trade-for", player,
            Util.getItemStackName(Objects.requireNonNull(e.getItem()))));
      });
    
    Util.debug(ChatColor.YELLOW + "> Handled interact");
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    /*
     * Workaround for GH-303
     * 
     * This will cause NPE when the internal getLocation method
     * itself NPE, which should be a server issue.
     */
    try {
      @Nullable Location chest = event.getInventory().getLocation();
      
      if (chest != null)
        ShopManager.instance().getLoadedShopAt(chest).ifPresent(Shop::setSignText);
      
    } catch (NullPointerException npe) {
      return;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {
    if (BaseConfig.autoFetchShopMessages)
      MsgUtil.flushMessagesFor(event.getPlayer());
  }
  
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent e) {
    ShopActionManager.instance().getActions().remove(e.getPlayer().getUniqueId());
  }
  
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDyingSign(PlayerInteractEvent event) {
    @Nullable Block block = event.getClickedBlock();
    @Nullable ItemStack item = event.getItem();
    
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || item == null)
      return;

    // This method will only match by using modern material name,
    // since this will only happen after 1.14
    if (!Util.isDyes(item.getType()))
      return;

    Util.getShopBySign(block).ifPresent(() -> {
      event.setCancelled(true);
      Util.debug("Disallow " + event.getPlayer().getName() + " dye the shop sign.");
    });
  }
  
  /*
   * Movement handlers
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMove(PlayerMoveEvent event) {
    handlePlayerMovement(event.getPlayer(), event.getTo());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeleport(PlayerTeleportEvent event) {
    handlePlayerMovement(event.getPlayer(), event.getTo());
  }
  
  public void handlePlayerMovement(Player player, Location to) {
    ShopData info = ShopActionManager.instance().getActions().get(player.getUniqueId());
    if (info == null)
      return;

    final Location shopLoc = info.location().bukkit();

    if (shopLoc.getWorld() != to.getWorld() || shopLoc.distanceSquared(to) > 25) {
      if (info.action() == ShopAction.CREATE)
        player.sendMessage(MsgUtil.getMessage("shop-creation-cancelled", player));
      else
        player.sendMessage(MsgUtil.getMessage("shop-purchase-cancelled", player));
      
      Util.debug(player.getName() + " too far with the shop location.");
      ShopActionManager.instance().getActions().remove(player.getUniqueId());
    }
  }
}
