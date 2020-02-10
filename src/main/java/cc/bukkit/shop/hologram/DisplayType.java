package cc.bukkit.shop.hologram;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.hologram.ArmorStandDisplayItem;
import org.maxgamer.quickshop.hologram.RealDisplayItem;
import lombok.Getter;
import lombok.experimental.Accessors;

public enum DisplayType {
  /*
   * UNKNOWN = FALLBACK TO REALITEM REALITEM = USE REAL DROPPED ITEM ARMORSTAND = USE ARMORSTAND
   * DISPLAY VIRTUALITEM = USE VIRTUAL DROPPED ITEM (CLIENT SIDE)
   */
  UNKNOWN(-1, EntityType.UNKNOWN), // same as 0

  DROPPED_ITEM(0, EntityType.DROPPED_ITEM),
  
  ARMOR_STAND(1, EntityType.ARMOR_STAND),

  VIRTUAL_ITEM(2, EntityType.UNKNOWN); // not implement yet

  private int id;
  
  @Getter
  @Accessors(fluent = true)
  private EntityType entityType;

  DisplayType(int id, EntityType type) {
    this.id = id;
    this.entityType = type;
  }

  public static @NotNull DisplayType fromID(int id) {
    for (DisplayType type : DisplayType.values()) {
      if (type.id == id) {
        return type;
      }
    }
    return UNKNOWN;
  }

  public static int toID(@NotNull DisplayType displayType) {
    return displayType.id;
  }

  public static DisplayType typeIs(@Nullable DisplayItem displayItem) {
    if (displayItem instanceof RealDisplayItem) {
      return DROPPED_ITEM;
    }
    if (displayItem instanceof ArmorStandDisplayItem) {
      return ARMOR_STAND;
    }
    return UNKNOWN;
  }

  public int toID() {
    return id;
  }
}
