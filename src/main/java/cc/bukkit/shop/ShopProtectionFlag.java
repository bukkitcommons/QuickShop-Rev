package cc.bukkit.shop;

import java.io.Serializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.Util;
import lombok.Data;

@Data
public class ShopProtectionFlag implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_MARK = "QuickShop DisplayItem";
  
  private final String shopItemStackData;
  private final String shopLocationData;
  private String mark = defaultMark();
  
  /**
   * Create the shop protection flag for display item.
   *
   * @param itemStack The item stack
   * @param shop The shop
   * @return ShopProtectionFlag obj
   */
  public static ShopProtectionFlag create(
      @NotNull ItemStack itemStack,
      @NotNull ContainerShop shop) {
    return new ShopProtectionFlag(Util.serialize(itemStack), shop.getLocation().toString());
  }

  public static String defaultMark() {
    return DEFAULT_MARK;
  }
}
