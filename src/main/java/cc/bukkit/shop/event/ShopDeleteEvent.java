package cc.bukkit.shop.event;

import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import cc.bukkit.shop.ContainerShop;
import lombok.Getter;

public class ShopDeleteEvent extends ShopEvent implements Cancellable {

  @Getter
  private final boolean fromMemory;

  @Getter
  @NotNull
  private final ContainerShop shop;

  private boolean cancelled;

  /**
   * Call the event when shop is deleteing. The ShopUnloadEvent will call after ShopDeleteEvent
   *
   * @param sho Target shop
   * @param fromMemory Only delete from the memory? false = delete both in memory and database
   */
  public ShopDeleteEvent(@NotNull ContainerShop shop, boolean fromMemory) {
    this.shop = shop;
    this.fromMemory = fromMemory;
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
