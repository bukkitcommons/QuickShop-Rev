package org.maxgamer.quickshop.shop;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public interface ShopData {
  public ItemStack item();
  public Location location();
  public ShopAction action();
}
