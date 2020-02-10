package cc.bukkit.shop.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import cc.bukkit.shop.Shop;

/**
 * Called when the price of a shop changes for some reason.
 */
@Getter
@AllArgsConstructor
public class ShopPriceChangeEvent extends ShopEvent implements Cancellable {

  /**
   * The new price that the shop will be set to.
   */
  @Setter
  private double newPrice;

  /**
   * The old price now and before this change.
   */
  private double oldPrice;

  @Setter
  private boolean cancelled;

  @NotNull
  private final Shop shop;
  
  @NotNull
  private final Reason reason;

  public enum Reason {
    UNKNOWN,
    RESTRICT;
  }
}
