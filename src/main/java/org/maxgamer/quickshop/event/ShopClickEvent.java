package org.maxgamer.quickshop.event;

import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.shop.api.Shop;

public class ShopClickEvent extends ShopEvent implements Cancellable {

  @NotNull
  @Getter
  private final Shop shop;

  private boolean cancelled;

  /**
   * Call when shop was clicked.
   *
   * @param shop The shop bought from
   */
  public ShopClickEvent(@NotNull Shop shop) {
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
