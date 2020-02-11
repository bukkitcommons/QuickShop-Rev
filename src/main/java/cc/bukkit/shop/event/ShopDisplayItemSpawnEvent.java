package cc.bukkit.shop.event;

import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.hologram.DisplayData;
import cc.bukkit.shop.hologram.DisplayType;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ShopDisplayItemSpawnEvent extends ShopEvent implements Cancellable {
  @NotNull
  private final DisplayType displayType;
  
  @NotNull
  private final DisplayData displayData;

  @NotNull
  private final ItemStack itemStack;

  @NotNull
  private final ContainerShop shop;

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
      @NotNull ContainerShop shop,
      @NotNull ItemStack itemStack,
      @NotNull DisplayData data) {
    
    this.displayData = data;
    this.shop = shop;
    this.itemStack = itemStack;
    
    this.displayType = data.type();
  }

  /**
   * This event is called before the shop display item created
   *
   * @param shop Target shop
   * @param itemStack The ItemStack for spawning the displayItem
   */
  public ShopDisplayItemSpawnEvent(@NotNull ContainerShop shop, @NotNull ItemStack itemStack) {
    this(shop, itemStack, DisplayData.create(itemStack));
  }
}
