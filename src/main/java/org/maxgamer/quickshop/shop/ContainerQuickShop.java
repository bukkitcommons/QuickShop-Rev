package org.maxgamer.quickshop.shop;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.DisplayConfig;
import org.maxgamer.quickshop.hologram.ArmorStandDisplay;
import org.maxgamer.quickshop.hologram.DisplayDataMatcher;
import org.maxgamer.quickshop.hologram.DisplayDroppedItem;
import org.maxgamer.quickshop.hologram.EntityDisplay;
import org.maxgamer.quickshop.utils.BlockUtils;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.ShopUtils;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Lists;
import com.google.common.graph.ElementOrder.Type;
import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.GenericChestShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.event.ShopLoadEvent;
import cc.bukkit.shop.event.ShopModeratorChangedEvent;
import cc.bukkit.shop.event.ShopPriceChangeEvent;
import cc.bukkit.shop.event.ShopPriceChangeEvent.Reason;
import cc.bukkit.shop.event.ShopSaveEvent;
import cc.bukkit.shop.event.ShopUnloadEvent;
import cc.bukkit.shop.hologram.DisplayData;
import cc.bukkit.shop.hologram.DisplayScheme;
import cc.bukkit.shop.hologram.Display;
import cc.bukkit.shop.hologram.DisplayAttribute;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.misc.ShopLocation;
import cc.bukkit.shop.moderator.ShopModerator;
import cc.bukkit.shop.stack.ItemStacked;
import cc.bukkit.shop.stack.Stack;
import cc.bukkit.shop.stack.StackItem;
import cc.bukkit.shop.stack.Stacked;
import cc.bukkit.shop.viewer.ShopViewer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public abstract class ContainerQuickShop implements GenericChestShop {
  @NotNull
  protected final ItemStacked stack;
  @NotNull
  protected final ShopLocation location;
  @Nullable
  protected EntityDisplay display;
  
  @EqualsAndHashCode.Exclude
  protected boolean isLoaded = false;
  
  protected ShopModerator moderator;
  protected Stacked price;
  protected DisplayData displayData;
  protected boolean unlimited;
  
  @NotNull
  @Override
  public abstract ShopType type();
  
  @Override
  public @NotNull DisplayScheme scheme() {
    return (@NotNull DisplayScheme) displayData;
  }
  
  @Override
  public ItemStack stack() {
    return stack.stack();
  }

  @Override
  public Stacked price() {
    return price;
  }

  @Override
  public @NotNull ShopModerator moderator() {
    return moderator;
  }

  @Override
  public @NotNull ShopLocation location() {
    return location;
  }

  protected ContainerQuickShop(@NotNull ContainerQuickShop s) {
    this.displayData = s.displayData;
    this.unlimited = s.unlimited;
    this.price = s.price;
    
    this.stack = StackItem.of(new ItemStack(s.stack()));
    this.location = s.location.clone();
    this.moderator = s.moderator.clone();
    
    if (DisplayConfig.displayItems) {
      DisplayData data = DisplayDataMatcher.create(this.stack.stack());
      switch (data.type()) {
        case ARMOR_STAND:
          display = new ArmorStandDisplay(this, data);
          break;
        default:
          display = new DisplayDroppedItem(this);
          break;
      }
    }
  }
  
  public ContainerQuickShop(@NotNull ShopLocation shopLocation, Stack price, @NotNull ItemStacked item,
      @NotNull ShopModerator moderator, boolean unlimited, @NotNull ShopType type) {
    this(shopLocation, price, item, moderator, unlimited, type, true);
  }

  /**
   * Adds a new shop.
   *
   * @param shopLocation The location of the chest block
   * @param price The cost per item
   * @param item The itemstack with the properties we want. This is .cloned, no need to worry about
   *        references
   * @param moderator The modertators
   * @param type The shop type
   * @param unlimited The unlimited
   */
  public ContainerQuickShop(@NotNull ShopLocation shopLocation, Stack price, @NotNull ItemStacked item,
      @NotNull ShopModerator moderator, boolean unlimited, @NotNull ShopType type, boolean spawnDisplay) {
    this.location = shopLocation;
    this.price = price;
    this.moderator = moderator;
    this.stack = StackItem.of(new ItemStack(item.stack()));
    this.unlimited = unlimited;

    if (spawnDisplay) {
      if (DisplayConfig.displayItems) {
        DisplayData data = DisplayDataMatcher.create(this.stack.stack());
        switch (data.type()) {
          case ARMOR_STAND:
            display = new ArmorStandDisplay(this, data);
            break;
          default:
            display = new DisplayDroppedItem(this);
            break;
        }
      }
    }
  }

  /**
   * Add an item to shops chest.
   *
   * @param stack The itemstack. The amount does not matter, just everything else
   * @param amount The amount to add to the shop.
   */
  @Override
  public void fill(int amount) {
    if (unlimited)
      return;
    
    Inventory inv = getInventory();
    ItemStack offer = new ItemStack(stack.stack());
    
    while (amount --> 0)
      inv.addItem(offer);
    
    setSignText();
  }

  /**
   * Returns the number of items this shop has in stock.
   *
   * @return The number of items available for purchase.
   */
  @Override
  public int getRemainingStock() {
    return unlimited ? -1 : ShopUtils.countStacks(getInventory(), stack.stack());
  }

  /**
   * Returns the number of free spots in the chest for the particular item.
   *
   * @return remaining space
   */
  @Override
  public int getRemainingSpace() {
    return unlimited ? -1 : ShopUtils.countSpace(getInventory(), stack.stack());
  }

  /**
   * Returns true if the ItemStack matches what this shop is selling/buying
   *
   * @param stack The ItemStack
   * @return True if the ItemStack is the same (Excludes amounts)
   */
  @Override
  public boolean isStack(@Nullable Object that) {
    if (that instanceof ItemStack)
      return QuickShop.instance().getItemMatcher().matches(stack.stack(), (ItemStack) that, false);
    else
      return false;
  }

  /**
   * Sets the price of the shop.
   *
   * @param price The new price of the shop.
   */
  @Override
  public void setPrice(Stack newPrice) {
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
                  stack.stack(),
                  unlimited ? 1 : 0,
                  displayData.type().id(),
                  price.stack(),
                  location.x(),
                  location.y(),
                  location.z(),
                  location.worldName());
      
    } catch (Throwable t) {
      ShopLogger.instance().severe(
          "Could not update a shop in the database! Changes will revert after a reboot!");
      
      t.printStackTrace();
    }
  }

  /**
   * Returns the shop that shares it's inventory with this one.
   *
   * @return the shop that shares it's inventory with this one. Will return null if this shop is not
   *         attached to another.
   */
  @Nullable
  public ContainerQuickShop getAttachedShop() {
    Optional<Location> c = BlockUtils.getSecondHalf(location.block());
    if (!c.isPresent())
      return null;
    
    ShopViewer shop = Shop.getManager().getLoadedShopAt(c.get());
    return (ContainerQuickShop) shop.get();
  }

  @Nullable
  public Inventory getInventory() {
    try {
      if (QuickShop.instance().getOpenInvPlugin().isPresent() &&
          location.block().getType() == Material.ENDER_CHEST) {
        
        com.lishid.openinv.OpenInv openInv = ((com.lishid.openinv.OpenInv) QuickShop.instance().getOpenInvPlugin().get());
        
        return openInv.getSpecialEnderChest(
            openInv.loadPlayer(Bukkit.getOfflinePlayer(moderator.getOwner())),
            Bukkit.getOfflinePlayer((moderator.getOwner())).isOnline()).getBukkitInventory();
      }
    } catch (InstantiationException ex) {
      ShopLogger.instance().warning("This exception was throwed by OpenInv and probably not a QuickShop issue.");
      ex.printStackTrace();
    }
    
    try {
      InventoryHolder container = (InventoryHolder) location.block().getState();
      return container.getInventory();
    } catch (Throwable t) {
      ShopLogger.instance().severe("The container of a shop have probably gone, with current block type: " +
          location.block().getType() + " @ " + location);
      
      Shop.getManager().unload(this);
      return null;
    }
  }

  /**
   * Changes all lines of text on a sign near the shop
   *
   * @param lines The array of lines to change. Index is line number.
   */
  private void setSignText(@NotNull String[] lines) {
    for (Sign sign : getAttached()) {
      if (Arrays.equals(sign.getLines(), lines))
        continue;
      
      for (int i = 0; i < lines.length; i++)
        sign.setLine(i, lines[i]);
      
      sign.update(true);
    }
  }

  @Override
  public void setUnlimited(boolean unlimited) {
    this.unlimited = unlimited;
    setSignText();
    save();
  }

  /** Updates signs attached to the shop */
  @Override
  public void setSignText() {
    if (!Util.isChunkLoaded(location))
      return;
    
    String[] lines = new String[4];
    
    OfflinePlayer player =
        QuickShop.instance().getPlaceHolderAPI().isPresent() && QuickShop.instance().getPlaceHolderAPI().get().isEnabled() ?
        Bukkit.getOfflinePlayer(getOwner()) : null;
    
    lines[0] = Shop.getLocaleManager().get(
        "signs.header",
        player,
        ownerName());
    
    String section = type().name().toLowerCase();
    lines[1] = Shop.getLocaleManager().get(
        "signs.".concat(section),
        player,
        unlimited ?
            Shop.getLocaleManager().get("signs.unlimited", player) :
            (String.valueOf(type() == ShopType.SELLING ? getRemainingStock() : getRemainingSpace()))
        );
    
    String stacks = stack.stack().getAmount() > 1 ? " * " + stack.stack().getAmount() : "";
    lines[2] = Shop.getLocaleManager().get(
        "signs.item",
        player,
        ItemUtils.getItemStackName(stack.stack()) + stacks);
    
    lines[3] = Shop.getLocaleManager().get(
        "signs.price",
        player,
        price.stack());
    
    setSignText(lines);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Shop "
        + (location.world() == null ? "unloaded world" : location.world().getName()) + "("
        + location.x() + ", " + location.y() + ", " + location.z() + ")");
    sb.append(" Owner: ").append(this.ownerName()).append(" - ").append(getOwner());
    if (isUnlimited()) {
      sb.append(" Unlimited: true");
    }
    sb.append(" Price: ").append(getPrice());
    sb.append(" Item: ").append(stack.stack());
    return sb.toString();
  }
  
  private static final BlockFace[] SIGN_FACES =
    {BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST};

  /**
   * Returns a list of signs that are attached to this shop (QuickShop and blank signs only)
   *
   * @return a list of signs that are attached to this shop (QuickShop and blank signs only)
   */
  @NotNull
  @Override
  public List<Sign> getAttached() {
    OfflinePlayer player = Bukkit.getOfflinePlayer(getOwner());
    String signHeader =
        Shop.getLocaleManager().get("signs.header", player, ownerName());
    
    List<Sign> signs = Lists.newArrayListWithCapacity(4);

    Block chest = location.block();
    for (BlockFace face : SIGN_FACES) {
      Block block = chest.getRelative(face);
      
      if (!BlockUtils.isWallSign(block.getType()) || !isAttached(block))
        continue;
      
      Sign sign = (Sign) block.getState();
      String[] lines = sign.getLines();
      
      Util.debug("Line: " + lines[0] + ChatColor.RESET + ", Header: " + signHeader);
      
      if (lines[0].equals(signHeader) ||
          Arrays.stream(lines).allMatch(String::isEmpty))
        signs.add(sign);
    }
    
    return signs;
  }

  @Override
  public boolean isAttached(@NotNull Block sign) {
    return location.bukkit().equals(BlockUtils.getSignAttached(sign).get().getLocation());
  }

  /**
   * Returns true if this shop is a double chest, and the other half is selling/buying the same as
   * this is buying/selling.
   *
   * @return true if this shop is a double chest, and the other half is selling/buying the same as
   *         this is buying/selling.
   */
  public boolean isDualShop() {
    ContainerQuickShop nextTo = getAttachedShop();
    if (nextTo == null)
      return false;
    
    if (nextTo.isStack(stack.stack())) {
      return type() != nextTo.type();
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
    return BlockUtils.isDoubleChest(this.getLocation().block());
  }

  @Override
  @Nullable
  public Display display() {
    return this.display;
  }

  @Override
  public void load() {
    if (isLoaded || Util.fireCancellableEvent(new ShopLoadEvent(this)))
      return;

    isLoaded = true;
    
    // check price restriction // FIXME FIX FEATURE
    /*
    Entry<Double, Double> priceRestriction = Util.getPriceRestriction(this.item.getType());

    if (priceRestriction != null) {
      double min = priceRestriction.getKey();
      double max = priceRestriction.getValue();
      double fix = price < min ? min : (price > max ? max : price);
      
      price = fix;
      save();
    }
    */
    
    setSignText();
    //checkDisplay();
  }

  @Override
  public void unload() {
    if (!isLoaded)
      return;
    
    if (display != null) {
      //display.remove();
    }
    
    save();
    isLoaded = false;
    
    ShopUnloadEvent shopUnloadEvent = new ShopUnloadEvent(this);
    Bukkit.getPluginManager().callEvent(shopUnloadEvent);
  }

  @Override
  public @NotNull String ownerName() {
    if (unlimited) {
      return Shop.getLocaleManager().get("admin-shop",
          Bukkit.getOfflinePlayer(this.getOwner()));
    }
    
    String name = Bukkit.getOfflinePlayer(getOwner()).getName();
    if (name == null || name.isEmpty()) {
      return Shop.getLocaleManager().get("unknown-owner",
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
