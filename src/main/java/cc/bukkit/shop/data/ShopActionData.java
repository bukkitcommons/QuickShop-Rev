package cc.bukkit.shop.data;

import org.bukkit.inventory.ItemStack;

public interface ShopActionData {
  public ShopLocation location();
  
  public ItemStack item();
  
  public ShopAction action();
}
