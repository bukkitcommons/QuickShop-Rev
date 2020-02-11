package cc.bukkit.shop.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ShopInventoryPreviewEvent extends ShopEvent implements Cancellable {

  @Setter
  @NotNull
  private ItemStack itemStack;

  @NotNull
  private final Player player;

  @Setter
  private boolean cancelled;

  /**
   * Build a event when player using GUI preview
   *
   * @param player Target plugin
   * @param itemStack The preview item, with preview flag.
   */
  public ShopInventoryPreviewEvent(@NotNull Player player, @NotNull ItemStack itemStack) {
    this.player = player;
    this.itemStack = itemStack;
  }
}
