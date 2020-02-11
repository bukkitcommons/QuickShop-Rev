package cc.bukkit.shop.data;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.ItemMatcher;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.ShopModerator;
import cc.bukkit.shop.ShopType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ShopSnapshot implements ShopActionData {
  private final ItemStack item;
  private final ShopLocation location;
  private final ShopModerator moderator;
  private final double price;
  private final ShopType shopType;
  private final boolean unlimited;
  
  public ShopSnapshot(@NotNull ContainerShop shop) {
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
  public boolean hasChanged(@NotNull ContainerShop shop) {
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
