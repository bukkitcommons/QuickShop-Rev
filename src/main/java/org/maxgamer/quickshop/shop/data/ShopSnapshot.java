package org.maxgamer.quickshop.shop.data;

import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.ShopModerator;
import org.maxgamer.quickshop.shop.ShopType;
import org.maxgamer.quickshop.utils.ItemMatcher;

@Data
@Accessors(fluent = true)
public class ShopSnapshot implements ShopData {
  private final ItemStack item;
  private final Location location;
  private final ShopModerator moderator;
  private final double price;
  private final ShopType shopType;
  private final boolean unlimited;
  
  public ShopSnapshot(@NotNull Shop shop) {
    item = shop.getItem();
    location = shop.getLocation();
    moderator = shop.getModerator();
    price = shop.getPrice();
    shopType = shop.getShopType();
    unlimited = shop.isUnlimited();
  }

  /**
   * Get shop is or not has changed.
   *
   * @param shop, The need checked with this shop.
   * @return hasChanged
   */
  public boolean hasChanged(@NotNull Shop shop) {
    return
        unlimited != shop.isUnlimited() ||
        shopType != shop.getShopType() ||
        price != shop.getPrice() ||
        !moderator.equals(shop.getModerator()) ||
        !shop.getLocation().equals(shop.getLocation()) ||
        !new ItemMatcher().matches(item, shop.getItem());
  }

  @Override
  public ShopAction action() {
    return ShopAction.TRADE;
  }
}
