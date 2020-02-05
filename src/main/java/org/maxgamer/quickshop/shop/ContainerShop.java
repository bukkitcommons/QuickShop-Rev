package org.maxgamer.quickshop.shop;

import com.lishid.openinv.OpenInv;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.event.ShopClickEvent;
import org.maxgamer.quickshop.event.ShopDeleteEvent;
import org.maxgamer.quickshop.event.ShopLoadEvent;
import org.maxgamer.quickshop.event.ShopModeratorChangedEvent;
import org.maxgamer.quickshop.event.ShopPriceChangeEvent;
import org.maxgamer.quickshop.event.ShopPriceChangeEvent.Reason;
import org.maxgamer.quickshop.event.ShopUnloadEvent;
import org.maxgamer.quickshop.event.ShopSaveEvent;
import org.maxgamer.quickshop.shop.api.Managed;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopModerator;
import org.maxgamer.quickshop.shop.api.ShopType;
import org.maxgamer.quickshop.shop.hologram.DisplayData;
import org.maxgamer.quickshop.shop.hologram.DisplayItem;
import org.maxgamer.quickshop.shop.hologram.impl.ArmorStandDisplayItem;
import org.maxgamer.quickshop.shop.hologram.impl.RealDisplayItem;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

/** ChestShop core */
@Getter
@Setter
@EqualsAndHashCode
public class ContainerShop implements Shop, Managed {
  @NotNull
  private final ItemStack item;
  @NotNull
  private final Location location;
  @Nullable
  private DisplayItem displayItem;
  
  @EqualsAndHashCode.Exclude
  private boolean isLoaded = false;
  
  private ShopModerator moderator;
  private double price;
  private ShopType shopType;
  private boolean unlimited;

  private ContainerShop(@NotNull ContainerShop s) {
    this.displayItem = s.displayItem;
    this.shopType = s.shopType;
    this.item = s.item;
    this.location = s.location;
    this.unlimited = s.unlimited;
    this.moderator = s.moderator;
    this.price = s.price;
    this.isLoaded = s.isLoaded;
  }

  /**
   * Adds a new shop.
   *
   * @param location The location of the chest block
   * @param price The cost per item
   * @param item The itemstack with the properties we want. This is .cloned, no need to worry about
   *        references
   * @param moderator The modertators
   * @param type The shop type
   * @param unlimited The unlimited
   */
  public ContainerShop(@NotNull Location location, double price, @NotNull ItemStack item,
      @NotNull ShopModerator moderator, boolean unlimited, @NotNull ShopType type) {
    this.location = location;
    this.price = price;
    this.moderator = moderator;
    this.item = item;
    this.shopType = type;
    this.unlimited = unlimited;

    if (BaseConfig.displayItems) {
      DisplayData data = DisplayData.getDisplayData(this.item);
      switch (data.type) {
        case UNKNOWN:
          Util.debugLog(
              "Failed to create a ContainerShop displayItem, the type is unknown, fallback to RealDisplayItem");
          this.displayItem = new RealDisplayItem(this);
          break;
        case REALITEM:
          this.displayItem = new RealDisplayItem(this);
          break;
        case ARMORSTAND:
          this.displayItem = new ArmorStandDisplayItem(this, data);
          break;
        default:
          Util.debugLog(
              "Warning: Failed to create a ContainerShop displayItem, the type we didn't know, fallback to RealDisplayItem");
          this.displayItem = new RealDisplayItem(this);
          break;
      }
    } else {
      Util.debugLog("The display was disabled.");
    }
  }

  /**
   * Add an item to shops chest.
   *
   * @param item The itemstack. The amount does not matter, just everything else
   * @param amount The amount to add to the shop.
   */
  @Override
  public void add(@NotNull ItemStack item, int amount) {
    if (this.unlimited) {
      return;
    }
    Inventory inv = this.getInventory();
    int remains = amount;
    while (remains > 0) {
      int stackSize = Math.min(remains, item.getMaxStackSize());
      item.setAmount(stackSize);
      inv.addItem(item);
      remains -= stackSize;
    }
    this.setSignText();
  }

  /**
   * Returns the number of items this shop has in stock.
   *
   * @return The number of items available for purchase.
   */
  @Override
  public int getRemainingStock() {
    return unlimited ? -1 : Util.countItems(this.getInventory(), this.getItem());
  }

  /**
   * Returns the number of free spots in the chest for the particular item.
   *
   * @return remaining space
   */
  @Override
  public int getRemainingSpace() {
    return unlimited ? -1 : Util.countSpace(this.getInventory(), this.getItem());
  }

  /**
   * Returns true if the ItemStack matches what this shop is selling/buying
   *
   * @param item The ItemStack
   * @return True if the ItemStack is the same (Excludes amounts)
   */
  @Override
  public boolean isShoppingItem(@Nullable ItemStack item) {
    return QuickShop.instance().getItemMatcher().matches(this.item, item);
  }

  /**
   * Sets the price of the shop.
   *
   * @param price The new price of the shop.
   */
  @Override
  public void setPrice(double newPrice) {
    ShopPriceChangeEvent event = new ShopPriceChangeEvent(newPrice, price, false, this, Reason.RESTRICT);
    if (Util.fireCancellableEvent(event))
      return;
    
    price = event.getNewPrice();
    setSignText();
    save();
  }

  /** Upates the shop into the database. */
  @Override
  public void save() {
    if (Util.fireCancellableEvent(new ShopSaveEvent(this)))
      return;

    try {
      QuickShop.instance()
               .getDatabaseHelper()
               .updateShop(
                  moderator.serialize(),
                  item,
                  unlimited ? 1 : 0,
                  shopType.toID(),
                  price,
                  location.getBlockX(),
                  location.getBlockY(),
                  location.getBlockZ(),
                  location.getWorld().getName());
      
    } catch (Throwable t) {
      ShopLogger.instance().severe(
          "Could not update a shop in the database! Changes will revert after a reboot!");
      
      t.printStackTrace();
    }
  }

  /** @return The durability of the item */
  @Override
  public short getDurability() {
    return (short) ((Damageable) this.item.getItemMeta()).getDamage();
  }

  /**
   * Buys amount of item from Player p. Does NOT check our inventory, or balances
   *
   * @param p The player to buy from
   * @param amount The amount to buy
   */
  @Override
  public void buy(@NotNull Player p, int amount) {
    if (amount < 0) {
      this.sell(p, -amount);
    }
    amount = amount * this.item.getAmount();
    if (this.isUnlimited()) {
      ItemStack[] contents = p.getInventory().getContents();
      for (int i = 0; amount > 0 && i < contents.length; i++) {
        ItemStack stack = contents[i];
        if (stack == null || stack.getType() == Material.AIR) {
          continue; // No item
        }
        if (isShoppingItem(stack)) {
          int stackSize = Math.min(amount, stack.getAmount());
          stack.setAmount(stack.getAmount() - stackSize);
          amount -= stackSize;
        }
      }
      // Send the players new inventory to them
      p.getInventory().setContents(contents);
      this.setSignText();
      // This should not happen.
      if (amount > 0) {
        ShopLogger.instance().log(Level.WARNING,
            "Could not take all items from a players inventory on purchase! " + p.getName()
                + ", missing: " + amount + ", item: " + Util.getItemStackName(this.getItem())
                + "!");
      }
    } else {
      ItemStack[] playerContents = p.getInventory().getContents();
      Inventory chestInv = this.getInventory();
      for (int i = 0; amount > 0 && i < playerContents.length; i++) {
        ItemStack item = playerContents[i];
        if (item != null && this.isShoppingItem(item)) {
          // Copy it, we don't want to interfere
          item = new ItemStack(item);
          // Amount = total, item.getAmount() = how many items in the
          // stack
          int stackSize = Math.min(amount, item.getAmount());
          // If Amount is item.getAmount(), then this sets the amount
          // to 0
          // Else it sets it to the remainder
          playerContents[i].setAmount(playerContents[i].getAmount() - stackSize);
          // We can modify this, it is a copy.
          item.setAmount(stackSize);
          // Add the items to the players inventory
          Objects.requireNonNull(chestInv).addItem(item);
          amount -= stackSize;
        }
      }
      // Now update the players inventory.
      p.getInventory().setContents(playerContents);
      this.setSignText();
    }
  }

  /**
   * Deletes the shop from the list of shops and queues it for database deletion
   *
   * @param fromMemory True if you are *NOT* iterating over this currently, *false if you are
   *        iterating*
   */
  @Override
  public void delete() {
    ShopDeleteEvent shopDeleteEvent = new ShopDeleteEvent(this, false);
    if (Util.fireCancellableEvent(shopDeleteEvent)) {
      Util.debugLog("Shop deletion was canceled because a plugin canceled it.");
      return;
    }
    
    // Unload the shop
    if (isLoaded)
      this.onUnload();
    
    // Delete the display item
    if (getDisplayItem() != null) {
      getDisplayItem().remove();
    }
    
    // Delete the signs around it
    for (Sign s : this.getSigns())
      s.getBlock().setType(Material.AIR);
    
    // Delete it from the database
    int x = this.getLocation().getBlockX();
    int y = this.getLocation().getBlockY();
    int z = this.getLocation().getBlockZ();
    String world = this.getLocation().getWorld().getName();
    // Refund if necessary
    if (BaseConfig.refundable)
      QuickShop.instance().getEconomy().deposit(this.getOwner(), BaseConfig.refundCost); // FIXME
    
    try {
      ShopManager.instance().unloadShop(this);
      QuickShop.instance().getDatabaseHelper().deleteShop(x, y, z, world);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void checkDisplay() {
    try {
      // Workaround for error tracing when this is executed by tasks
      checkDisplay0();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private void checkDisplay0() {
    if (!BaseConfig.displayItems || !this.isLoaded) { // FIXME: Reinit scheduler on reloading config
      return;
    }

    if (this.displayItem == null) {
      Util.debugLog("Warning: DisplayItem is null, this shouldn't happend...");
      Util.debugLog("Call from: " + Thread.currentThread().getStackTrace()[2].getClassName() + "#"
          + Thread.currentThread().getStackTrace()[2].getMethodName() + "%"
          + Thread.currentThread().getStackTrace()[2].getLineNumber());
      return;
    }

    if (!this.displayItem.isSpawned()) {
      /* Not spawned yet. */
      Util.debugLog("Target item not spawned, spawning...");
      this.displayItem.spawn();
    } else {
      this.displayItem.fixPosition();
    }

    /* Dupe is always need check, if enabled display */
    this.displayItem.removeDupe();
    // plugin.getDisplayDupeRemoverWatcher().add(this.displayItem);
  }

  /**
   * Removes an item from the shop.
   *
   * @param item The itemstack. The amount does not matter, just everything else
   * @param amount The amount to remove from the shop.
   */
  @Override
  public void remove(@NotNull ItemStack item, int amount) {
    if (this.unlimited) {
      return;
    }
    Inventory inv = this.getInventory();
    int remains = amount;
    while (remains > 0) {
      int stackSize = Math.min(remains, item.getMaxStackSize());
      item.setAmount(stackSize);
      Objects.requireNonNull(inv).removeItem(item);
      remains -= stackSize;
    }
    this.setSignText();
  }

  /**
   * Returns a clone of this shop. References to the same display item, itemstack, location and
   * owner as this shop does. Do not modify them or you will modify this shop.
   *
   * <p>
   * **NOT A DEEP CLONE**
   */
  @Override
  @NotNull
  public ContainerShop clone() {
    return new ContainerShop(this);
  }

  /**
   * Sells amount of item to Player p. Does NOT check our inventory, or balances
   *
   * @param p The player to sell to
   * @param amount The amount to sell
   */
  @Override
  public void sell(@NotNull Player p, int amount) {
    if (amount < 0) {
      this.buy(p, -amount);
    }
    // Items to drop on floor
    ArrayList<ItemStack> floor = new ArrayList<>(5);
    Inventory pInv = p.getInventory();
    amount = amount * this.item.getAmount();
    if (this.isUnlimited()) {
      ItemStack item = new ItemStack(this.item);
      while (amount > 0) {
        int stackSize = Math.min(amount, this.item.getMaxStackSize());
        item.setAmount(stackSize);
        pInv.addItem(item);
        amount -= stackSize;
      }
    } else {
      ItemStack[] chestContents = Objects.requireNonNull(this.getInventory()).getContents();
      for (int i = 0; amount > 0 && i < chestContents.length; i++) {
        // Can't clone it here, it could be null
        ItemStack item = chestContents[i];
        if (item != null && item.getType() != Material.AIR && this.isShoppingItem(item)) {
          // Copy it, we don't want to interfere
          item = new ItemStack(item);
          // Amount = total, item.getAmount() = how many items in the
          // stack
          int stackSize = Math.min(amount, item.getAmount());
          // If Amount is item.getAmount(), then this sets the amount
          // to 0
          // Else it sets it to the remainder
          chestContents[i].setAmount(chestContents[i].getAmount() - stackSize);
          // We can modify this, it is a copy.
          item.setAmount(stackSize);
          // Add the items to the players inventory
          floor.addAll(pInv.addItem(item).values());
          amount -= stackSize;
        }
      }
      // We now have to update the chests inventory manually.
      this.getInventory().setContents(chestContents);
      this.setSignText();
    }
    for (ItemStack stack : floor) {
      p.getWorld().dropItem(p.getLocation(), stack);
    }
  }

  /**
   * Returns the shop that shares it's inventory with this one.
   *
   * @return the shop that shares it's inventory with this one. Will return null if this shop is not
   *         attached to another.
   */
  @Nullable
  public ContainerShop getAttachedShop() {
    Block c = Util.getSecondHalf(this.getLocation().getBlock());
    if (c == null) {
      return null;
    }
    ShopViewer shop = ShopManager.instance().getShop(c.getLocation());
    return (ContainerShop) shop.get();
  }

  /** @return The chest this shop is based on. */
  public @Nullable Inventory getInventory() {
    try {
      if (location.getBlock().getState().getType() == Material.ENDER_CHEST
          && QuickShop.instance().getOpenInvPlugin() != null) {
        OpenInv openInv = ((OpenInv) QuickShop.instance().getOpenInvPlugin());
        return openInv.getSpecialEnderChest(
            Objects.requireNonNull(
                openInv.loadPlayer(Bukkit.getOfflinePlayer(this.moderator.getOwner()))),
            Bukkit.getOfflinePlayer((this.moderator.getOwner())).isOnline()).getBukkitInventory();
      }
    } catch (Exception e) {
      Util.debugLog(e.getMessage());
      return null;
    }
    InventoryHolder container;
    try {
      container = (InventoryHolder) this.location.getBlock().getState();
      return container.getInventory();
    } catch (Exception e) {
      this.onUnload();
      this.delete();
      Util.debugLog("Inventory doesn't exist anymore: " + this + " shop was removed.");
      return null;
    }
  }

  /**
   * Changes all lines of text on a sign near the shop
   *
   * @param lines The array of lines to change. Index is line number.
   */
  @Override
  public void setSignText(@NotNull String[] lines) {
    for (Sign sign : this.getSigns()) {
      if (Arrays.equals(sign.getLines(), lines)) {
        Util.debugLog("Skipped new sign text setup: Same content");
        continue;
      }
      for (int i = 0; i < lines.length; i++) {
        sign.setLine(i, lines[i]);
      }
      sign.update(true);
    }
  }

  @Override
  public void setUnlimited(boolean unlimited) {
    this.unlimited = unlimited;
    this.setSignText();
    save();
  }

  /**
   * Changes a shop type to Buying or Selling. Also updates the signs nearby.
   *
   * @param shopType The new type (ShopType.BUYING or ShopType.SELLING)
   */
  @Override
  public void setShopType(@NotNull ShopType shopType) {
    this.shopType = shopType;
    this.setSignText();
    save();
  }

  /** Updates signs attached to the shop */
  @Override
  public void setSignText() {
    if (!Util.isChunkLoaded(this.location)) // FIXME check
      return;
    
    String[] lines = new String[4];
    
    OfflinePlayer player =
        QuickShop.instance().getPlaceHolderAPI() != null && QuickShop.instance().getPlaceHolderAPI().isEnabled() ?
        Bukkit.getOfflinePlayer(this.getOwner()) : null;
    
    lines[0] = MsgUtil.getMessagePlaceholder(
        "signs.header",
        player,
        ownerName());
    
    String section = shopType.name().toLowerCase();
    lines[1] = MsgUtil.getMessagePlaceholder(
        "signs.".concat(section),
        player,
        unlimited ?
            MsgUtil.getMessagePlaceholder("signs.unlimited", player) :
            String.valueOf(getRemainingSpace()));
    
    lines[2] = MsgUtil.getMessagePlaceholder(
        "signs.item",
        player,
        Util.getItemStackName(getItem()));
    
    lines[3] = MsgUtil.getMessagePlaceholder(
        "signs.price",
        player,
        Util.format(this.getPrice()));
    
    this.setSignText(lines);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Shop "
        + (location.getWorld() == null ? "unloaded world" : location.getWorld().getName()) + "("
        + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")");
    sb.append(" Owner: ").append(this.ownerName()).append(" - ").append(getOwner());
    if (isUnlimited()) {
      sb.append(" Unlimited: true");
    }
    sb.append(" Price: ").append(getPrice());
    sb.append(" Item: ").append(getItem());
    return sb.toString();
  }

  /**
   * Returns a list of signs that are attached to this shop (QuickShop and blank signs only)
   *
   * @return a list of signs that are attached to this shop (QuickShop and blank signs only)
   */
  @Override
  public @NotNull List<Sign> getSigns() {
    List<Sign> signs = new ArrayList<>(1);
    if (this.getLocation().getWorld() == null) {
      return signs;
    }
    Block[] blocks = new Block[4];
    blocks[0] = location.getBlock().getRelative(BlockFace.EAST);
    blocks[1] = location.getBlock().getRelative(BlockFace.NORTH);
    blocks[2] = location.getBlock().getRelative(BlockFace.SOUTH);
    blocks[3] = location.getBlock().getRelative(BlockFace.WEST);
    OfflinePlayer player = Bukkit.getOfflinePlayer(this.getOwner());
    final String signHeader =
        MsgUtil.getMessagePlaceholder("sign.header", player, this.ownerName());

    for (Block b : blocks) {
      if (b == null) {
        ShopLogger.instance().warning("Null signs in the queue, skipping");
        continue;
      }
      Material mat = b.getType();
      if (!Util.isWallSign(mat)) {
        continue;
      }
      if (!isAttached(b)) {
        continue;
      }
      if (!(b.getState() instanceof Sign)) {
        continue;
      }
      org.bukkit.block.Sign sign = (org.bukkit.block.Sign) b.getState();
      String[] lines = sign.getLines();
      for (int i = 0; i < lines.length; i++) {
        if (i == 0) {
          if (lines[i].contains(signHeader)) {
            signs.add(sign);
            break;
          }
        } else {
          if (Arrays.stream(lines).anyMatch((str) -> !str.isEmpty())) {
            break;
          }
        }
      }
      signs.add(sign);
    }
    return signs;
  }

  @Override
  public boolean isAttached(@NotNull Block b) {
    return this.getLocation().getBlock().equals(Util.getAttached(b));
  }

  /**
   * Returns true if this shop is a double chest, and the other half is selling/buying the same as
   * this is buying/selling.
   *
   * @return true if this shop is a double chest, and the other half is selling/buying the same as
   *         this is buying/selling.
   */
  public boolean isDoubleShop() {
    ContainerShop nextTo = this.getAttachedShop();
    if (nextTo == null) {
      return false;
    }
    if (nextTo.isShoppingItem(this.getItem())) {
      // They're both trading the same item
      // They're both buying or both selling => Not a double shop,
      // just two shops.
      // One is buying, one is selling.
      return this.getShopType() != nextTo.getShopType();
    } else {
      return false;
    }
  }

  /**
   * Different with isDoubleShop, this method only check the shop is created on the double chest.
   *
   * @return true if create on double chest.
   */
  public boolean isDoubleChestShop() {
    return Util.isDoubleChest(this.getLocation().getBlock());
  }

  /**
   * Check shop is or not still Valid.
   *
   * @return isValid
   */
  @Override
  public boolean isValid() {
    this.checkDisplay();
    return Util.canBeShop(this.getLocation().getBlock());
  }

  @Override
  public @Nullable DisplayItem getDisplay() {
    return this.displayItem;
  }

  /** Check the container still there and we can keep use it. */
  public void checkContainer() {
    if (!this.isLoaded) {
      return;
    }
    if (!Util.canBeShop(this.getLocation().getBlock())) {
      Util.debugLog("Shop at " + this.getLocation() + " container was missing, remove...");
      this.onUnload();
      this.delete();
    }
  }

  /** Load ContainerShop. */
  @Override
  public void onLoad() {
    if (this.isLoaded || Util.fireCancellableEvent(new ShopLoadEvent(this)))
      return;

    this.isLoaded = true;
    ShopManager
      .instance()
      .getLoadedShops().add(this);
    
    // check price restriction // FIXME move
    Entry<Double, Double> priceRestriction = Util.getPriceRestriction(this.item.getType());

    if (priceRestriction != null) {
      double min = priceRestriction.getKey();
      double max = priceRestriction.getValue();
      double fix = price < min ? min : (price > max ? max : price);
      
      price = fix;
      save();
    }
    
    checkDisplay();
  }

  /** Unload ContainerShop. */
  @Override
  public void onUnload() {
    if (!this.isLoaded) {
      Util.debugLog("Dupe unload request, canceled.");
      return;
    }
    if (this.getDisplayItem() != null) {
      this.getDisplayItem().remove();
    }
    save();
    this.isLoaded = false;
    ShopManager.instance().getLoadedShops().remove(this);
    ShopUnloadEvent shopUnloadEvent = new ShopUnloadEvent(this);
    Bukkit.getPluginManager().callEvent(shopUnloadEvent);
  }

  @Override
  public void onClick() {
    ShopClickEvent event = new ShopClickEvent(this);
    if (Util.fireCancellableEvent(event)) {
      Util.debugLog("Ignore shop click, because some plugin cancel it.");
      return;
    }
    this.setSignText();
    this.checkDisplay();
  }

  @Override
  public @NotNull String ownerName() {
    if (this.isUnlimited()) {
      return MsgUtil.getMessagePlaceholder("admin-shop",
          Bukkit.getOfflinePlayer(this.getOwner()));
    }
    String name = Bukkit.getOfflinePlayer(this.getOwner()).getName();
    if (name == null || name.isEmpty()) {
      return MsgUtil.getMessagePlaceholder("unknown-owner",
          Bukkit.getOfflinePlayer(this.getOwner()));
    }
    return name;
  }
  
  
  /*
   * Shop Moderator
   */
  @NotNull
  @Override
  public Set<UUID> getStaffs() {
    return this.moderator.getStaffs();
  }
  
  @Override
  public boolean isModerator(@NotNull UUID player) {
    return moderator.isModerator(player);
  }

  @Override
  public boolean isOwner(@NotNull UUID player) {
    return moderator.isOwner(player);
  }

  @Override
  public boolean isStaff(@NotNull UUID player) {
    return moderator.isStaff(player);
  }

  @Override
  public boolean removeStaff(@NotNull UUID player) {
    boolean result = this.moderator.removeStaff(player);
    save();
    if (result) {
      Bukkit.getPluginManager().callEvent(new ShopModeratorChangedEvent(this, this.moderator));
    }
    return result;
  }
  
  @Override
  public @NotNull UUID getOwner() {
    return this.moderator.getOwner();
  }

  @Override
  public void setOwner(@NotNull UUID owner) {
    this.moderator.setOwner(owner);
    Bukkit.getPluginManager().callEvent(new ShopModeratorChangedEvent(this, this.moderator));
    this.setSignText();
    save();
  }

  @Override
  public boolean addStaff(@NotNull UUID player) {
    boolean result = this.moderator.addStaff(player);
    save();
    if (result) {
      Bukkit.getPluginManager().callEvent(new ShopModeratorChangedEvent(this, this.moderator));
    }
    return result;
  }
  
  @Override
  public void clearStaffs() {
    this.moderator.clearStaffs();
    Bukkit.getPluginManager().callEvent(new ShopModeratorChangedEvent(this, this.moderator));
    save();
  }
}
