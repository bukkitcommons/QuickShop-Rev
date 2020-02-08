package org.maxgamer.quickshop.shop.hologram;

import com.google.gson.Gson;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.shop.api.ShopProtectionFlag;
import org.maxgamer.quickshop.utils.Util;

public class DisplayItemPersistentDataType
    implements PersistentDataType<String, ShopProtectionFlag> {
  public static final DisplayItemPersistentDataType INSTANCE = new DisplayItemPersistentDataType();
  private static Gson gson = new Gson();

  @Override
  public @NotNull Class<String> getPrimitiveType() {
    return String.class;
  }

  @Override
  public @NotNull Class<ShopProtectionFlag> getComplexType() {
    return ShopProtectionFlag.class;
  }

  @NotNull
  @Override
  public String toPrimitive(@NotNull ShopProtectionFlag complex,
      @NotNull PersistentDataAdapterContext context) {
    try {
      return gson.toJson(complex);
    } catch (Throwable th) {
      new RuntimeException("Cannot to toPrimitive the shop protection flag.").printStackTrace();
      return "";
    }
  }

  @NotNull
  @Override
  public ShopProtectionFlag fromPrimitive(@NotNull String primitive,
      @NotNull PersistentDataAdapterContext context) {
    try {
      return gson.fromJson(primitive, ShopProtectionFlag.class);
    } catch (Throwable th) {
      new RuntimeException("Cannot to fromPrimitive the shop protection flag.").printStackTrace();
      return new ShopProtectionFlag("", Util.serialize(new ItemStack(Material.STONE)));
    }
  }
}
