package cc.bukkit.shop;

import java.io.Serializable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import lombok.Data;

@Data
public class DisplayInfo implements Serializable {
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
  public static DisplayInfo from(
      @NotNull ItemStack itemStack,
      @NotNull ContainerShop shop) {
    return new DisplayInfo(serializeItemStack(itemStack), shop.getLocation().toString());
  }

  public static String defaultMark() {
    return DEFAULT_MARK;
  }
  
  private final static YamlConfiguration SERIALIZER = new YamlConfiguration();
  
  /**
   * Covert ItemStack to YAML string.
   *
   * @param iStack target ItemStack
   * @return String serialized itemStack
   */
  private static String serializeItemStack(@NotNull ItemStack iStack) {
    SERIALIZER.set("item", iStack);
    return SERIALIZER.saveToString();
  }
}
