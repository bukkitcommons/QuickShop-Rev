package cc.bukkit.shop.data;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ShopModerator;
import cc.bukkit.shop.ShopType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Data
@RequiredArgsConstructor
@Accessors(fluent = true)
public class ShopData implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @NotNull
  private final String item;
  @NotNull
  private final String moderators;
  @NotNull
  private final String world;
  @NotNull
  private final ShopType type;
  private final double price;
  private final boolean unlimited;
  private final int x;
  private final int y;
  private final int z;
  
  @Nullable
  private transient ShopModerator manager;
  
  @NotNull
  public ShopModerator moderators() {
    return manager == null ?
        ShopModerator.deserialize(moderators) : manager;
  }
  
  @Nullable
  private transient ItemStack stack;
  
  @Nullable
  public ItemStack item() {
    try {
      return stack == null ?
          Util.deserialize(item) : stack;
    } catch (InvalidConfigurationException e) {
      return null;
    }
  }
  
  @Nullable
  private transient ShopLocation location;
  
  @Nullable
  public ShopLocation location() {
    return location == null ? ShopLocation.from(world, x, y, z) : location;
  }
  
  public ShopData(@NotNull ResultSet rs) throws SQLException {
    this.x = rs.getInt("x");
    this.y = rs.getInt("y");
    this.z = rs.getInt("z");
    this.price = rs.getDouble("price");
    this.unlimited = rs.getBoolean("unlimited");
    this.type = ShopType.fromID(rs.getInt("type"));
    this.world = rs.getString("world");
    this.item = rs.getString("itemConfig");
    this.moderators = rs.getString("owner");
  }
}
