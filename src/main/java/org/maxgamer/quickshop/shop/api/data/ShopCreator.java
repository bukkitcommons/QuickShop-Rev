package org.maxgamer.quickshop.shop.api.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Data
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public class ShopCreator implements ShopData {
  @NotNull
  private final ShopLocation location;
  @NotNull
  private final Block sign;
  @NotNull
  private final ItemStack item;

  @Override
  public ShopAction action() {
    return ShopAction.CREATE;
  }
}
