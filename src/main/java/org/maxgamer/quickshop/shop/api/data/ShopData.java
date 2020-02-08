package org.maxgamer.quickshop.shop.api.data;

import org.bukkit.inventory.ItemStack;

public interface ShopData {
  public ItemStack item();
  public ShopLocation location();
  public ShopAction action();
}
