package cc.bukkit.shop;

import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.shop.QuickShopManager;
import cc.bukkit.shop.data.ShopLocation;
import cc.bukkit.shop.hologram.DisplayItem;

public interface ContainerShop extends Managed {
  /**
   * Delete the shop from database.
   * @see QuickShopManager#delete(ContainerShop)
   */
  @Deprecated
  default public void delete() {
    Shop.getLoader().delete(this);
  }
  
  /**
   * Helper to check shop type.
   * @param shopType type
   * @return whether is that type.
   */
  default boolean is(@NotNull ShopType shopType) {
    return getShopType() == shopType;
  }
  
  /**
   * Gets moderators information of this shop.
   * @return the moderator
   */
  ShopModerator getModerator();
  
  /**
   * Clone the shop by its constructor.
   * @return the cloned shop
   */
  @NotNull
  @Deprecated
  ContainerShop clone();
  
  /**
   * Invokes on shop being loaded.
   * @see QuickShopManager#load(String, ContainerShop)
   */
  void onLoad();

  /**
   * Invokes on shop being unloaded.
   * @see QuickShopManager#unload(ContainerShop)
   */
  void onUnload();
  
  void fill(int paramInt);

  /**
   * Execute buy action for player with x items.
   *
   * @param paramPlayer Target player
   * @param paramInt How many buyed?
   */
  void buy(Player paramPlayer, int paramInt);

  /** Check the display location, and teleport, respawn if needs. */
  void checkDisplay();

  /**
   * Check shop is or not attacked the target block
   *
   * @param paramBlock Target block
   * @return isAttached
   */
  boolean isAttached(Block paramBlock);

  /**
   * Check the target ItemStack is matches with this shop's item.
   *
   * @param paramItemStack Target ItemStack.
   * @return Matches
   */
  boolean isShoppingItem(ItemStack paramItemStack);

  /** Execute codes when player click the shop will did things */
  void onClick();

  /**
   * Get shop's owner name, it will return owner name or Admin Shop(i18n) when it is unlimited
   *
   * @return owner name
   */
  @NotNull
  String ownerName();

  /**
   * Execute sell action for player with x items.
   *
   * @param paramPlayer Target player
   * @param paramInt How many sold?
   */
  void sell(Player paramPlayer, int paramInt);

  /** Generate new sign texts on shop's sign. */
  void setSignText();

  /** Update shop data to database */
  void save();

  /**
   * Get shop's item durability, if have.
   *
   * @return Shop's item durability
   */
  short getDurability();

  /**
   * Get shop item's ItemStack
   *
   * @return The shop's ItemStack
   */
  @NotNull
  ItemStack getItem();

  /**
   * Get shop's location
   *
   * @return Shop's location
   */
  @NotNull
  ShopLocation getLocation();

  /**
   * Get shop's price
   *
   * @return Price
   */
  double getPrice();

  /**
   * Set shop's new price
   *
   * @param paramDouble New price
   */
  void setPrice(double paramDouble);

  /**
   * Get shop remaining space.
   *
   * @return Remaining space.
   */
  int getRemainingSpace();

  /**
   * Get shop remaining stock.
   *
   * @return Remaining stock.
   */
  int getRemainingStock();

  /**
   * Get shop type
   *
   * @return shop type
   */
  @NotNull
  ShopType getShopType();

  /**
   * Set new shop type for this shop
   *
   * @param paramShopType New shop type
   */
  void setShopType(ShopType paramShopType);

  /**
   * Get shop signs, may have multi signs
   *
   * @return Signs for the shop
   */
  @NotNull
  List<Sign> getShopSigns();

  /**
   * Get this container shop is loaded or unloaded.
   *
   * @return Loaded
   */
  boolean isLoaded();

  /**
   * Get shop is or not in Unlimited Mode (Admin Shop)
   *
   * @return yes or not
   */
  boolean isUnlimited();

  /**
   * Set shop is or not Unlimited Mode (Admin Shop)
   *
   * @param paramBoolean status
   */
  void setUnlimited(boolean paramBoolean);

  /**
   * Shop is valid
   *
   * @return status
   */
  boolean isValid();

  /**
   * Get the shop display entity
   *
   * @return The entity for shop display.
   */
  @Nullable
  DisplayItem getDisplay();
}
