package cc.bukkit.shop.hologram;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public enum DisplayType {
  UNKNOWN(-1, EntityType.UNKNOWN),

  DROPPED_ITEM(0, EntityType.DROPPED_ITEM),
  
  ARMOR_STAND(1, EntityType.ARMOR_STAND),

  VIRTUAL_ITEM(2, EntityType.UNKNOWN);

  private final int id;
  @NotNull
  private final EntityType entityType;

  public static @NotNull DisplayType fromID(int id) {
    for (DisplayType type : DisplayType.values()) {
      if (type.id == id) {
        return type;
      }
    }
    return UNKNOWN;
  }
}
