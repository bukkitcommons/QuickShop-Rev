package cc.bukkit.shop.event;

import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import cc.bukkit.shop.ContainerShop;
import lombok.Getter;

public class ShopClickEvent extends ShopEvent implements Cancellable {

  @NotNull
  @Getter
  private final ContainerShop shop;

  private boolean cancelled;

  /**
   * Call when shop was clicked.
   *
   * @param shop The shop bought from
   */
  public ShopClickEvent(@NotNull ContainerShop shop) {
    this.shop = shop;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
