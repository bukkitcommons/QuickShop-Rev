package org.maxgamer.quickshop.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.hologram.DisplayItem;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

public class DisplayProtectionListener implements Listener {
  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void block(BlockFromToEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    Block targetBlock = event.getToBlock();
    Block shopBlock = targetBlock.getRelative(BlockFace.DOWN);
    ShopViewer shop = ShopManager.instance().getLoadedShopFrom(shopBlock.getLocation());
    if (!shop.isPresent()) {
      return;
    }
    event.setCancelled(true);
    if (shop.get().getDisplay() != null) {
      shop.get().getDisplay().remove();
    }
    sendAlert("[DisplayGuard] Liuqid " + targetBlock.getLocation()
        + " trying flow to top of shop, QuickShop already cancel it.");
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void portal(EntityPortalEvent event) {
    if (!(event.getEntity() instanceof Item)) {
      return;
    }
    if (DisplayItem.isDisplayItem(((Item) event.getEntity()).getItemStack())) {
      event.setCancelled(true);
      event.getEntity().remove();
      sendAlert("[DisplayGuard] Somebody want dupe the display by Portal at " + event.getFrom()
          + " , QuickShop already cancel it.");
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void block(BlockPistonExtendEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    Block block = event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.DOWN);
    ShopViewer shop = ShopManager.instance().getLoadedShopFrom(block.getLocation());
    if (shop.isPresent()) {
      event.setCancelled(true);
      sendAlert("[DisplayGuard] Piston  " + event.getBlock().getLocation()
          + " trying push somethings on the shop top, QuickShop already cancel it.");
      if (shop.get().getDisplay() != null) {
        shop.get().getDisplay().remove();
      }
      return;
    }
    for (Block oBlock : event.getBlocks()) {
      Block otherBlock = oBlock.getRelative(event.getDirection()).getRelative(BlockFace.DOWN);
      if (Util.canBeShop(otherBlock)) {
        shop = ShopManager.instance().getLoadedShopFrom(otherBlock.getLocation());
        if (shop != null) {
          event.setCancelled(true);
          sendAlert("[DisplayGuard] Piston  " + event.getBlock().getLocation()
              + " trying push somethings on the shop top, QuickShop already cancel it.");
          if (shop.get().getDisplay() != null) {
            shop.get().getDisplay().remove();
          }
          return;
        }
      }
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void block(BlockPistonRetractEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    Block block = event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.DOWN);
    ShopViewer shop = ShopManager.instance().getLoadedShopFrom(block.getLocation());
    if (shop.get() != null) {
      event.setCancelled(true);
      sendAlert("[DisplayGuard] Piston  " + event.getBlock().getLocation()
          + " trying pull somethings on the shop top, QuickShop already cancel it.");
      if (shop.get().getDisplay() != null) {
        shop.get().getDisplay().remove();
      }
      return;
    }
    for (Block oBlock : event.getBlocks()) {
      Block otherBlock = oBlock.getRelative(event.getDirection()).getRelative(BlockFace.DOWN);
      if (Util.canBeShop(otherBlock)) {
        shop = ShopManager.instance().getLoadedShopFrom(otherBlock.getLocation());
        if (shop.get() != null) {
          event.setCancelled(true);
          sendAlert("[DisplayGuard] Piston  " + event.getBlock().getLocation()
              + " trying push somethings on the shop top, QuickShop already cancel it.");
          if (shop.get().getDisplay() != null) {
            shop.get().getDisplay().remove();
          }
          return;
        }
      }
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void block(BrewingStandFuelEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    ItemStack itemStack = event.getFuel();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
      sendAlert("[DisplayGuard] Block  " + event.getBlock().getLocation()
          + " trying fuel the BrewingStand with DisplayItem.");
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void block(FurnaceBurnEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    ItemStack itemStack = event.getFuel();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
      Block furnace = event.getBlock();
      if (furnace.getState() instanceof Furnace) {
        Furnace furnace1 = (Furnace) furnace.getState();
        sendAlert("[DisplayGuard] Block  " + event.getBlock().getLocation()
            + " trying burn with DisplayItem.");
        Util.inventoryCheck(furnace1.getInventory());
      }
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void block(FurnaceSmeltEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    ItemStack itemStack = event.getSource();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
      Block furnace = event.getBlock();
      if (furnace.getState() instanceof Furnace) {
        Furnace furnace1 = (Furnace) furnace.getState();
        sendAlert("[DisplayGuard] Block  " + event.getBlock().getLocation()
            + " trying smelt with DisplayItem.");
        Util.inventoryCheck(furnace1.getInventory());
      }
      return;
    }
    itemStack = event.getResult();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
      Block furnace = event.getBlock();
      if (furnace.getState() instanceof Furnace) {
        Furnace furnace1 = (Furnace) furnace.getState();
        sendAlert("[DisplayGuard] Block  " + event.getBlock().getLocation()
            + " trying smelt with DisplayItem.");
        Util.inventoryCheck(furnace1.getInventory());
      }
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void entity(EntityPickupItemEvent e) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    ItemStack stack = e.getItem().getItemStack();
    if (!DisplayItem.isDisplayItem(stack)) {
      return;
    }
    e.setCancelled(true);
    // You shouldn't be able to pick up that...
    e.getItem().remove();
    sendAlert("[DisplayGuard] Entity " + e.getEntity().getName() + " # "
        + e.getEntity().getLocation() + " pickedup the displayItem, QuickShop already removed it.");

    Entity entity = e.getEntity();
    if (entity instanceof InventoryHolder) {
      Util.inventoryCheck(((InventoryHolder) entity).getInventory());
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void entity(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof ArmorStand)) {
      return;
    }
    if (!DisplayItem.isDisplayItem(((ArmorStand) event.getEntity()).getItemInHand())) {
      return;
    }
    event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void entity(EntityDeathEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    if (!(event.getEntity() instanceof ArmorStand)) {
      return;
    }
    if (!DisplayItem.isDisplayItem(((ArmorStand) event.getEntity()).getItemInHand())) {
      return;
    }
    event.setDroppedExp(0);
    event.getDrops().clear();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void entity(EntityInteractEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    if (!(event.getEntity() instanceof ArmorStand)) {
      return;
    }
    if (!DisplayItem.isDisplayItem(((ArmorStand) event.getEntity()).getItemInHand())) {
      return;
    }
    event.setCancelled(true);
    Entity entity = event.getEntity();
    if (entity instanceof InventoryHolder) {
      Util.inventoryCheck(((InventoryHolder) entity).getInventory());
    }
    sendAlert("[DisplayGuard] Entity  " + event.getEntityType().name() + " # "
        + event.getEntity().getLocation() + " trying interact the hold displayItem's entity.");
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void inventory(InventoryOpenEvent event) {
    Util.inventoryCheck(event.getInventory());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void inventory(InventoryClickEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    if (!DisplayItem.isDisplayItem(event.getCurrentItem())) {
      return;
    }
    if (event.getClickedInventory() == null) {
      return;
    }
    if (event.getClickedInventory().getLocation() == null) {
      return;
    }
    event.setCancelled(true);

    sendAlert("[DisplayGuard] Inventory " + event.getClickedInventory().getHolder() + " at"
        + event.getClickedInventory().getLocation()
        + " was clicked the displayItem, QuickShop already removed it.");
    event.getCurrentItem().setAmount(0);
    event.getCurrentItem().setType(Material.AIR);
    event.setResult(Result.DENY);
    Util.inventoryCheck(event.getInventory());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void inventory(InventoryPickupItemEvent event) {
    ItemStack itemStack = event.getItem().getItemStack();
    if (!DisplayItem.isDisplayItem(itemStack)) {
      return; // We didn't care that
    }
    @Nullable
    Location loc = event.getInventory().getLocation();
    @Nullable
    InventoryHolder holder = event.getInventory().getHolder();
    event.setCancelled(true);
    sendAlert("[DisplayGuard] Something  " + holder + " at " + loc
        + " trying pickup the DisplayItem,  you should teleport to that location and to check detail..");
    Util.inventoryCheck(event.getInventory());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void inventory(InventoryDragEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    ItemStack itemStack = event.getCursor();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
      Util.inventoryCheck(event.getInventory());
      sendAlert("[DisplayGuard] Player  " + event.getWhoClicked().getName()
          + " trying use DisplayItem crafting.");
      return;
    }
    itemStack = event.getOldCursor();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
      Util.inventoryCheck(event.getInventory());
      sendAlert("[DisplayGuard] Player  " + event.getWhoClicked().getName()
          + " trying use DisplayItem crafting.");
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void inventory(InventoryCreativeEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    ItemStack itemStack = event.getCursor();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
      Util.inventoryCheck(event.getInventory());
      sendAlert("[DisplayGuard] Player  " + event.getWhoClicked().getName()
          + " trying use DisplayItem crafting.");
      return;
    }
    itemStack = event.getCurrentItem();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
      Util.inventoryCheck(event.getInventory());
      sendAlert("[DisplayGuard] Player  " + event.getWhoClicked().getName()
          + " trying use DisplayItem crafting.");
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void item(PlayerItemHeldEvent e) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    ItemStack stack = e.getPlayer().getInventory().getItemInMainHand();
    ItemStack stackOffHand = e.getPlayer().getInventory().getItemInOffHand();
    if (DisplayItem.isDisplayItem(stack)) {
      e.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR, 0));
      // You shouldn't be able to pick up that...
      sendAlert("[DisplayGuard] Player " + e.getPlayer().getName()
          + " helded the displayItem, QuickShop already cancelled and removed it.");
      e.setCancelled(true);
      Util.inventoryCheck(e.getPlayer().getInventory());
    }
    if (DisplayItem.isDisplayItem(stackOffHand)) {
      e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.AIR, 0));
      // You shouldn't be able to pick up that...
      sendAlert("[DisplayGuard] Player " + e.getPlayer().getName()
          + " helded the displayItem, QuickShop already cancelled and removed it.");
      e.setCancelled(true);
      Util.inventoryCheck(e.getPlayer().getInventory());
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void item(ItemDespawnEvent event) {
    ItemStack itemStack = event.getEntity().getItemStack();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
    }

    // Util.debugLog("We canceled an Item from despawning because they are our display item.");
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void player(CraftItemEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    ItemStack itemStack;
    itemStack = event.getCurrentItem();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
      Util.inventoryCheck(event.getInventory());
      sendAlert("[DisplayGuard] Player  " + event.getWhoClicked().getName()
          + " trying use DisplayItem crafting.");
      return;
    }
    itemStack = event.getCursor();
    if (DisplayItem.isDisplayItem(itemStack)) {
      event.setCancelled(true);
      Util.inventoryCheck(event.getInventory());
      sendAlert("[DisplayGuard] Player  " + event.getWhoClicked().getName()
          + " trying use DisplayItem crafting.");
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void player(PlayerFishEvent event) {
    if (event.getState() != State.CAUGHT_ENTITY) {
      return;
    }
    if (event.getCaught() == null) {
      return;
    }
    if (event.getCaught().getType() != EntityType.DROPPED_ITEM) {
      return;
    }
    Item item = (Item) event.getCaught();
    ItemStack is = item.getItemStack();
    if (!DisplayItem.isDisplayItem(is)) {
      return;
    }
    // item.remove();
    event.getHook().remove();
    // event.getCaught().remove();
    event.setCancelled(true);
    sendAlert("[DisplayGuard] Player " + event.getPlayer().getName()
        + " trying hook item use Fishing Rod, QuickShop already removed it.");
    Util.inventoryCheck(event.getPlayer().getInventory());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void player(PlayerBucketEmptyEvent event) {
    if (!BaseConfig.enhancedDisplayProtection)
      return;
    
    Block waterBlock = event.getBlockClicked().getRelative(event.getBlockFace());
    ShopManager.instance().getLoadedShopAt(waterBlock.getRelative(BlockFace.DOWN).getLocation()).ifPresent(shop -> {
      event.setCancelled(true);
      sendAlert("[DisplayGuard] Player  " + event.getPlayer().getName()
          + " trying use water to move somethings on the shop top, QuickShop already remove it.");
    });
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void player(PlayerArmorStandManipulateEvent event) {
    if (!DisplayItem.isDisplayItem(event.getArmorStandItem())) {
      return;
    }
    event.setCancelled(true);
    Util.inventoryCheck(event.getPlayer().getInventory());
    sendAlert("[DisplayGuard] Player  " + event.getPlayer().getName()
        + " trying mainipulate armorstand contains displayItem.");
  }

  private void sendAlert(@NotNull String msg) {
    if (BaseConfig.enableAlert)
      MsgUtil.sendGlobalAlert(msg);
  }
}
