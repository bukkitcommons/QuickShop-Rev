package org.maxgamer.quickshop.shop.api.data;

import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@Accessors(fluent = true)
public class ShopCreator implements ShopData {
  @Nullable private final Block sign;
  @NotNull private final Location location;
  @Nullable private final ItemStack item;

  public ShopCreator(
      @NotNull Location loc,
      @Nullable ItemStack item,
      @Nullable Block last) {
    
    this.location = loc;
    this.sign = last;
    this.item = new ItemStack(item);
  }

  @Override
  public ShopAction action() {
    return ShopAction.CREATE;
  }
}
