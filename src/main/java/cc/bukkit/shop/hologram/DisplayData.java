package cc.bukkit.shop.hologram;

import java.util.Map;
import org.apache.commons.lang.ObjectUtils;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import com.google.common.collect.Maps;
import cc.bukkit.shop.hologram.DisplayAttribute;
import cc.bukkit.shop.hologram.DisplayType;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class DisplayData {
  @Getter
  private final DisplayType type;
  
  /**
   * Stores specificed attribute data
   * @see DisplayAttribute
   */
  private final Map<DisplayAttribute, Object> attribute = Maps.newEnumMap(DisplayAttribute.class);
  
  public void put(@NotNull DisplayAttribute attr, @NotNull Object object) {
    attribute.put(attr, object);
  }
  
  /**
   * Sets pose for a armor stand using the current display data
   * @param armorStand The armor stand to set pose for
   */
  public void setPoseForArmorStand(@NotNull ArmorStand armorStand) {
    armorStand.setBodyPose(new EulerAngle(get(DisplayAttribute.POSE_BODY_X, 0d),
        get(DisplayAttribute.POSE_BODY_Y, 0d),
        get(DisplayAttribute.POSE_BODY_Z, 0d)));

    armorStand.setHeadPose(new EulerAngle(get(DisplayAttribute.POSE_HEAD_X, 0d),
        get(DisplayAttribute.POSE_HEAD_Y, 0d),
        get(DisplayAttribute.POSE_HEAD_Z, 0d)));

    armorStand.setRightArmPose(new EulerAngle(get(DisplayAttribute.POSE_ARM_RIGHT_X, 0d),
        get(DisplayAttribute.POSE_ARM_RIGHT_Y, 0d),
        get(DisplayAttribute.POSE_ARM_RIGHT_Z, 0d)));

    armorStand.setLeftArmPose(new EulerAngle(get(DisplayAttribute.POSE_ARM_LEFT_X, 0d),
        get(DisplayAttribute.POSE_ARM_LEFT_Y, 0d),
        get(DisplayAttribute.POSE_ARM_LEFT_Z, 0d)));

    armorStand.setRightLegPose(new EulerAngle(get(DisplayAttribute.POSE_LEG_RIGHT_X, 0d),
        get(DisplayAttribute.POSE_LEG_RIGHT_Y, 0d),
        get(DisplayAttribute.POSE_LEG_RIGHT_Z, 0d)));

    armorStand.setLeftLegPose(new EulerAngle(get(DisplayAttribute.POSE_LEG_LEFT_X, 0d),
        get(DisplayAttribute.POSE_LEG_LEFT_Y, 0d),
        get(DisplayAttribute.POSE_LEG_LEFT_Z, 0d)));
  }
  
  /**
   * Obtains a value in the specific attribute slot from the current display data
   * @param <T> The object type in that attribute slot
   * @param attr The attribute slot
   * @param defaultValue The default value to return if no data specificed
   * @return The value of that attribute, may be the default value
   */
  @SuppressWarnings("unchecked")
  public <T> T get(DisplayAttribute attr, T defaultValue) {
    Object value = attribute.get(attr);
    if (value == ObjectUtils.NULL || value == null)
      return defaultValue;

    try {
      if (defaultValue instanceof EquipmentSlot) {
        return (T) EquipmentSlot.valueOf(String.class.cast(value));
      }

      if (value instanceof Integer) {
        if (defaultValue instanceof Double)
          return (T) Double.valueOf((int) value);

        if (defaultValue instanceof Float)
          return (T) Float.valueOf((int) value);
      }

      return (T) value;
    } catch (Throwable t) {
      ShopLogger.instance()
          .warning("Error when processing attribute for " + attr.name() + " with unexpected value "
              + value.toString() + ", please check your config before reporting!");
      t.printStackTrace();
      return defaultValue;
    }
  }
}
