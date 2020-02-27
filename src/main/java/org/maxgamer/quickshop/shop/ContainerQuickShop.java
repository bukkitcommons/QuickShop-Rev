package org.maxgamer.quickshop.shop;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.maxgamer.quickshop.utils.BlockUtils;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.ShopUtils;
import org.maxgamer.quickshop.utils.Util;

import com.google.common.collect.Lists;

import cc.bukkit.shop.BasicShop;
import cc.bukkit.shop.GenericChestShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.event.ShopLoadEvent;
import cc.bukkit.shop.event.ShopModeratorChangedEvent;
import cc.bukkit.shop.event.ShopPriceChangeEvent;
import cc.bukkit.shop.event.ShopPriceChangeEvent.Reason;
import cc.bukkit.shop.event.ShopUnloadEvent;
import cc.bukkit.shop.hologram.Display;
import cc.bukkit.shop.hologram.DisplayData;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.misc.ShopLocation;
import cc.bukkit.shop.moderator.ShopModerator;
import cc.bukkit.shop.stack.ItemStacked;
import cc.bukkit.shop.stack.StackItem;
import cc.bukkit.shop.viewer.ShopViewer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.experimental.Accessors;

@Getter
@Setter
@EqualsAndHashCode
@Accessors(fluent = true)
public abstract class ContainerQuickShop implements GenericChestShop {
    @NotNull
    protected final ItemStacked stack;
    @NotNull
    protected final ShopLocation location;
    @Nullable
    protected Display display;
    
    @EqualsAndHashCode.Exclude
    protected boolean isLoaded = false;
    
    protected ShopModerator moderator;
    protected double price;
    protected DisplayData displayData;
    protected boolean unlimited;
    
    @NotNull
    @Override
    public abstract ShopType type();
    
    @Override
    public @NotNull DisplayData data() {
        return displayData;
    }
    
    @Override
    public ItemStack stack() {
        return stack.stack();
    }
    
    @Override
    public Double price() {
        return price;
    }
    
    protected ContainerQuickShop(@NotNull ContainerQuickShop s) {
        displayData = s.displayData;
        unlimited = s.unlimited;
        price = s.price;
        
        stack = StackItem.of(new ItemStack(s.stack()));
        location = s.location.clone();
        moderator = s.moderator.clone();
        
        if (DisplayConfig.displayItems) {
            DisplayData data = DisplayDataMatcher.create(stack.stack());
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
    
    public ContainerQuickShop(@NotNull ShopLocation shopLocation, double price, @NotNull ItemStacked item, @NotNull ShopModerator moderator, boolean unlimited, @NotNull ShopType type) {
        this(shopLocation, price, item, moderator, unlimited, type, true);
    }
    
    /**
     * Adds a new shop.
     *
     * @param shopLocation The location of the chest block
     * @param price        The cost per item
     * @param item         The itemstack with the properties we want. This is
     *                     .cloned, no need to worry about references
     * @param moderator    The modertators
     * @param type         The shop type
     * @param unlimited    The unlimited
     */
    public ContainerQuickShop(@NotNull ShopLocation shopLocation, double price, @NotNull ItemStacked item, @NotNull ShopModerator moderator, boolean unlimited, @NotNull ShopType type, boolean spawnDisplay) {
        location = shopLocation;
        this.price = price;
        this.moderator = moderator;
        stack = StackItem.of(new ItemStack(item.stack()));
        this.unlimited = unlimited;
        
        if (spawnDisplay)
            if (DisplayConfig.displayItems) {
                DisplayData data = DisplayDataMatcher.create(stack.stack());
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
    
    @Override
    public void fill(int amount) {
        if (unlimited)
            return;
        
        val inv = getInventory();
        val offer = new ItemStack(stack.stack());
        
        while (amount-- > 0)
            inv.addItem(offer);
        
        setSignText();
    }
    
    @Override
    public int getRemainingStock() {
        return unlimited ? -1 : ShopUtils.countStacks(getInventory(), stack.stack());
    }
    
    @Override
    public int getRemainingSpace() {
        return unlimited ? -1 : ShopUtils.countSpaces(getInventory(), stack.stack());
    }
    
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
    public void setPrice(Object newPrice) {
        val event = new ShopPriceChangeEvent(newPrice, price, false, this, Reason.UNKNOWN);
        if (Util.callCancellableEvent(event))
            return;
        
        price = (double) event.getNewPrice();
        setSignText();
    }
    
    /**
     * Returns the shop that shares it's inventory with this one.
     *
     * @return the shop that shares it's inventory with this one. Will return null
     *         if this shop is not attached to another.
     */
    @NotNull
    public ShopViewer getAttachedShop() {
        val c = BlockUtils.getSecondHalf(location.block());
        if (c.isPresent())
            return Shop.getManager().getLoadedShopAt(c.get());
        else
            return ShopViewer.empty();
    }
    
    @Nullable
    public Inventory getInventory() {
        try {
            if (QuickShop.instance().getOpenInvPlugin().isPresent() && location.block().getType() == Material.ENDER_CHEST) {
                
                val openInv = ((com.lishid.openinv.OpenInv) QuickShop.instance().getOpenInvPlugin().get());
                
                return openInv.getSpecialEnderChest(openInv.loadPlayer(Bukkit.getOfflinePlayer(moderator.getOwner())), Bukkit.getOfflinePlayer((moderator.getOwner())).isOnline()).getBukkitInventory();
            }
        } catch (InstantiationException ex) {
            ShopLogger.instance().warning("This exception was throwed by OpenInv and probably not a QuickShop issue.");
            ex.printStackTrace();
        }
        
        try {
            return ((InventoryHolder) location.block().getState()).getInventory();
        } catch (Throwable t) {
            ShopLogger.instance().severe("The container of a shop have probably gone, with current block type: " + location.block().getType() + " @ " + location);
            
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
        for (val sign : getAttached()) {
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
    }
    
    /** Updates signs attached to the shop */
    @Override
    public void setSignText() {
        if (!Util.isChunkLoaded(location))
            return;
        
        val lines = new String[4];
        
        val player = QuickShop.instance().getPlaceHolderAPI().isPresent() && QuickShop.instance().getPlaceHolderAPI().get().isEnabled() ? Bukkit.getOfflinePlayer(getOwner()) : null;
        
        lines[0] = Shop.getLocaleManager().get("signs.header", player, ownerName());
        
        val section = type().name().toLowerCase();
        lines[1] = Shop.getLocaleManager().get("signs.".concat(section), player, unlimited ? Shop.getLocaleManager().get("signs.unlimited", player) : (String.valueOf(type() == ShopType.SELLING ? getRemainingStock() : getRemainingSpace())));
        
        val stacks = stack.stack().getAmount() > 1 ? " * " + stack.stack().getAmount() : "";
        lines[2] = Shop.getLocaleManager().get("signs.item", player, ItemUtils.getItemStackName(stack.stack()) + stacks);
        
        lines[3] = Shop.getLocaleManager().get("signs.price", player, price);
        
        setSignText(lines);
    }
    
    @Override
    public String toString() {
        val sb = new StringBuilder("Shop " + (location.world() == null ? "unloaded world" : location.world().getName()) + "(" + location.x() + ", " + location.y() + ", " + location.z() + ")");
        sb.append(" Owner: ").append(ownerName()).append(" - ").append(getOwner());
        if (unlimited())
            sb.append(" Unlimited: true");
        sb.append(" Price: ").append(price());
        sb.append(" Item: ").append(stack.stack());
        return sb.toString();
    }
    
    private static final BlockFace[] SIGN_FACES = { BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST };
    
    /**
     * Returns a list of signs that are attached to this shop (QuickShop and blank
     * signs only)
     *
     * @return a list of signs that are attached to this shop (QuickShop and blank
     *         signs only)
     */
    @NotNull
    @Override
    public List<Sign> getAttached() {
        val player = Bukkit.getOfflinePlayer(getOwner());
        val signHeader = Shop.getLocaleManager().get("signs.header", player, ownerName());
        
        List<Sign> signs = Lists.newArrayListWithCapacity(4);
        
        val chest = location.block();
        for (BlockFace face : SIGN_FACES) {
            Block block = chest.getRelative(face);
            
            if (!BlockUtils.isWallSign(block.getType()) || !isAttached(block))
                continue;
            
            Sign sign = (Sign) block.getState();
            String[] lines = sign.getLines();
            
            Util.trace("Line: " + lines[0] + ChatColor.RESET + ", Header: " + signHeader);
            
            if (lines[0].equals(signHeader) || Arrays.stream(lines).allMatch(String::isEmpty))
                signs.add(sign);
        }
        
        return signs;
    }
    
    @Override
    public boolean isAttached(@NotNull Block sign) {
        return location.bukkit().equals(BlockUtils.getSignAttached(sign).get().getLocation());
    }
    
    /**
     * Returns true if this shop is a double chest, and the other half is
     * selling/buying the same as this is buying/selling.
     *
     * @return true if this shop is a double chest, and the other half is
     *         selling/buying the same as this is buying/selling.
     */
    public ShopViewer converse() {
        ShopViewer nextTo = getAttachedShop();
        if (nextTo.isEmpty())
            return ShopViewer.empty();
        
        if (nextTo.get().isStack(stack.stack()))
            if (type() != nextTo.<BasicShop>get().type())
                return nextTo;
            else
                return ShopViewer.empty();
        else
            return ShopViewer.empty();
    }
    
    @Override
    public void load() {
        if (isLoaded || Util.callCancellableEvent(new ShopLoadEvent(this)))
            return;
        
        isLoaded = true;
        
        // check price restriction // FIXME FIX FEATURE
        /*
         * Entry<Double, Double> priceRestriction =
         * Util.getPriceRestriction(this.item.getType());
         * 
         * if (priceRestriction != null) { double min = priceRestriction.getKey();
         * double max = priceRestriction.getValue(); double fix = price < min ? min :
         * (price > max ? max : price);
         * 
         * price = fix; save(); }
         */
        
        setSignText();
        // checkDisplay();
    }
    
    @Override
    public void unload() {
        if (!isLoaded)
            return;
        
        if (display != null) {
            // display.remove();
        }
        
        isLoaded = false;
        
        ShopUnloadEvent shopUnloadEvent = new ShopUnloadEvent(this);
        Bukkit.getPluginManager().callEvent(shopUnloadEvent);
    }
    
    @Override
    public @NotNull String ownerName() {
        if (unlimited)
            return Shop.getLocaleManager().get("admin-shop", Bukkit.getOfflinePlayer(getOwner()));
        
        String name = Bukkit.getOfflinePlayer(getOwner()).getName();
        if (name == null || name.isEmpty())
            return Shop.getLocaleManager().get("unknown-owner", Bukkit.getOfflinePlayer(getOwner()));
        
        return name;
    }
    
    /*
     * Shop Moderator
     */
    @NotNull
    @Override
    public Set<UUID> getStaffs() {
        return moderator.getStaffs();
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
        val result = moderator.removeStaff(player);
        if (result)
            Bukkit.getPluginManager().callEvent(new ShopModeratorChangedEvent(this, moderator));
        return result;
    }
    
    @Override
    public @NotNull UUID getOwner() {
        return moderator.getOwner();
    }
    
    @Override
    public void setOwner(@NotNull UUID owner) {
        moderator.setOwner(owner);
        Bukkit.getPluginManager().callEvent(new ShopModeratorChangedEvent(this, moderator));
        setSignText();
    }
    
    @Override
    public boolean addStaff(@NotNull UUID player) {
        val result = moderator.addStaff(player);
        if (result)
            Bukkit.getPluginManager().callEvent(new ShopModeratorChangedEvent(this, moderator));
        return result;
    }
    
    @Override
    public void clearStaffs() {
        moderator.clearStaffs();
        Bukkit.getPluginManager().callEvent(new ShopModeratorChangedEvent(this, moderator));
    }
}
