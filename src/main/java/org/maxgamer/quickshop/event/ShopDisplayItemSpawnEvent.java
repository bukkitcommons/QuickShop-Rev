package org.maxgamer.quickshop.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.hologram.DisplayData;
import org.maxgamer.quickshop.shop.hologram.DisplayType;

@Getter
public class ShopDisplayItemSpawnEvent extends ShopEvent implements Cancellable {
  @NotNull
  private final DisplayType displayType;
  
  @NotNull
  private final DisplayData displayData;

  @NotNull
  private final ItemStack itemStack;

  @NotNull
  private final Shop shop;

  @Setter
  private boolean cancelled;

  /**
   * This event is called before the shop display item created
   *
   * @param shop Target shop
   * @param displayType The displayType
   * @param itemStack Target ItemStack
   */
  public ShopDisplayItemSpawnEvent(
      @NotNull Shop shop,
      @NotNull ItemStack itemStack,
      @NotNull DisplayData data) {
    
    this.displayData = data;
    this.shop = shop;
    this.itemStack = itemStack;
    this.displayType = data.type;
  }

  /**
   * This event is called before the shop display item created
   *
   * @param shop Target shop
   * @param itemStack The ItemStack for spawning the displayItem
   */
  @Deprecated
  public ShopDisplayItemSpawnEvent(@NotNull Shop shop, @NotNull ItemStack itemStack) {
    this(shop, itemStack, DisplayData.getDisplayData(itemStack));
  }
}
