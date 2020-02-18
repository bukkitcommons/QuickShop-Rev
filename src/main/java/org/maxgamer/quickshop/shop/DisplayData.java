package org.maxgamer.quickshop.shop;

import java.util.Map;
import org.apache.commons.lang.ObjectUtils;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.BlockUtils;
import com.google.common.collect.Maps;
import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.hologram.DisplayAttribute;
import cc.bukkit.shop.hologram.DisplayScheme;
import cc.bukkit.shop.hologram.DisplayType;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.misc.ShopLocation;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class DisplayData implements DisplayScheme {
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
  
  @SuppressWarnings("deprecation")
  public Location scheme(ChestShop shop) {
    ShopLocation location = shop.location();
    location.bukkit().clone().add(0, 1, 0); // FIXME
    
    BlockFace containerBlockFace = BlockFace.NORTH; // Set default vaule
    
    try {
      if (location.block().getBlockData() instanceof org.bukkit.block.data.Directional)
        containerBlockFace = ((org.bukkit.block.data.Directional) shop.location().block().getBlockData()).getFacing();
      
    } catch (Throwable t) {
      org.bukkit.material.MaterialData data = shop.location().block().getState().getData();
      if (data instanceof org.bukkit.material.Chest)
        containerBlockFace = ((org.bukkit.material.Chest) data).getFacing();
      else if (data instanceof org.bukkit.material.EnderChest)
        containerBlockFace = ((org.bukkit.material.EnderChest) data).getFacing();
    }

    Location asloc = BlockUtils.getCenter(shop.location());
    if (!shop.display().<ItemStack>sample().getType().isBlock())
      asloc.add(0, -0.5, 0);

    switch (containerBlockFace) {
      case WEST:
        asloc.setYaw(90);
        break;
      case EAST:
        asloc.setYaw(-90);
        break;
      case NORTH:
        asloc.setYaw(180);
        break;
      case SOUTH:
      default:
        break;
    }
    
    asloc.setYaw(asloc.getYaw() + attribute(DisplayAttribute.OFFSET_YAW, 0f));
    asloc.setPitch(asloc.getYaw() + attribute(DisplayAttribute.OFFSET_PITCH, 0f));
    
    asloc.add(
        attribute(DisplayAttribute.OFFSET_X, 0d),
        attribute(DisplayAttribute.OFFSET_Y, 0d),
        attribute(DisplayAttribute.OFFSET_Z, 0d));
    
    return asloc;
  }
  
  /**
   * Sets pose for a armor stand using the current display data
   * @param armorStand The armor stand to set pose for
   */
  public void setPoseForArmorStand(@NotNull ArmorStand armorStand) {
    armorStand.setBodyPose(new EulerAngle(attribute(DisplayAttribute.POSE_BODY_X, 0d),
        attribute(DisplayAttribute.POSE_BODY_Y, 0d),
        attribute(DisplayAttribute.POSE_BODY_Z, 0d)));

    armorStand.setHeadPose(new EulerAngle(attribute(DisplayAttribute.POSE_HEAD_X, 0d),
        attribute(DisplayAttribute.POSE_HEAD_Y, 0d),
        attribute(DisplayAttribute.POSE_HEAD_Z, 0d)));

    armorStand.setRightArmPose(new EulerAngle(attribute(DisplayAttribute.POSE_ARM_RIGHT_X, 0d),
        attribute(DisplayAttribute.POSE_ARM_RIGHT_Y, 0d),
        attribute(DisplayAttribute.POSE_ARM_RIGHT_Z, 0d)));

    armorStand.setLeftArmPose(new EulerAngle(attribute(DisplayAttribute.POSE_ARM_LEFT_X, 0d),
        attribute(DisplayAttribute.POSE_ARM_LEFT_Y, 0d),
        attribute(DisplayAttribute.POSE_ARM_LEFT_Z, 0d)));

    armorStand.setRightLegPose(new EulerAngle(attribute(DisplayAttribute.POSE_LEG_RIGHT_X, 0d),
        attribute(DisplayAttribute.POSE_LEG_RIGHT_Y, 0d),
        attribute(DisplayAttribute.POSE_LEG_RIGHT_Z, 0d)));

    armorStand.setLeftLegPose(new EulerAngle(attribute(DisplayAttribute.POSE_LEG_LEFT_X, 0d),
        attribute(DisplayAttribute.POSE_LEG_LEFT_Y, 0d),
        attribute(DisplayAttribute.POSE_LEG_LEFT_Z, 0d)));
  }
  
  /**
   * Obtains a value in the specific attribute slot from the current display data
   * @param <T> The object type in that attribute slot
   * @param attr The attribute slot
   * @param defaultValue The default value to return if no data specificed
   * @return The value of that attribute, may be the default value
   */
  @SuppressWarnings("unchecked")
  public <T> T attribute(DisplayAttribute attr, T defaultValue) {
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
