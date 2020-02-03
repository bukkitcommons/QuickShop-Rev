/*
 * This file is a part of project QuickShop, the name is DisplayData.java Copyright (C) Ghost_chu
 * <https://github.com/Ghost-chu> Copyright (C) Bukkit Commons Studio and contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.maxgamer.quickshop.shop.hologram;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang.ObjectUtils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DisplayData {
  public final DisplayType type;
  public final Map<DisplayAttribute, Object> attribute = Maps.newEnumMap(DisplayAttribute.class);
  @Deprecated
  public final boolean needVaildate; // Bad design
  public final boolean fixer;
  
  public static Location getCenter(Location loc) {
    // This is always '+' instead of '-' even in negative pos
    return new Location(loc.getWorld(), loc.getBlockX() + .5, loc.getBlockY() + .5,
        loc.getBlockZ() + .5);
  }
  
  public static void setPoseForArmorStand(@NotNull DisplayData data, ArmorStand armorStand) {
    armorStand.setBodyPose(new EulerAngle(getAttribute(data, DisplayAttribute.POSE_BODY_X, 0d),
        getAttribute(data, DisplayAttribute.POSE_BODY_Y, 0d),
        getAttribute(data, DisplayAttribute.POSE_BODY_Z, 0d)));

    armorStand.setHeadPose(new EulerAngle(getAttribute(data, DisplayAttribute.POSE_HEAD_X, 0d),
        getAttribute(data, DisplayAttribute.POSE_HEAD_Y, 0d),
        getAttribute(data, DisplayAttribute.POSE_HEAD_Z, 0d)));

    armorStand.setRightArmPose(new EulerAngle(getAttribute(data, DisplayAttribute.POSE_ARM_RIGHT_X, 0d),
        getAttribute(data, DisplayAttribute.POSE_ARM_RIGHT_Y, 0d),
        getAttribute(data, DisplayAttribute.POSE_ARM_RIGHT_Z, 0d)));

    armorStand.setLeftArmPose(new EulerAngle(getAttribute(data, DisplayAttribute.POSE_ARM_LEFT_X, 0d),
        getAttribute(data, DisplayAttribute.POSE_ARM_LEFT_Y, 0d),
        getAttribute(data, DisplayAttribute.POSE_ARM_LEFT_Z, 0d)));

    armorStand.setRightLegPose(new EulerAngle(getAttribute(data, DisplayAttribute.POSE_LEG_RIGHT_X, 0d),
        getAttribute(data, DisplayAttribute.POSE_LEG_RIGHT_Y, 0d),
        getAttribute(data, DisplayAttribute.POSE_LEG_RIGHT_Z, 0d)));

    armorStand.setLeftLegPose(new EulerAngle(getAttribute(data, DisplayAttribute.POSE_LEG_LEFT_X, 0d),
        getAttribute(data, DisplayAttribute.POSE_LEG_LEFT_Y, 0d),
        getAttribute(data, DisplayAttribute.POSE_LEG_LEFT_Z, 0d)));
  }
  
  @SuppressWarnings("unchecked")
  public static <T> T getAttribute(@NotNull DisplayData data, DisplayAttribute attr, T defaultValue) {
    Object value = data.attribute.get(attr);
    if (value == ObjectUtils.NULL || value == null)
      return defaultValue;
    Util.debugLog("Attribute to cast: " + value);

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
      QuickShop.instance().getLogger()
          .warning("Error when processing attribute for " + attr.name() + " with unexpected value "
              + value.toString() + ", please check your config before reporting!");
      t.printStackTrace();
      return defaultValue;
    }
  }
  
  /**
   * Get plugin now is using which one DisplayType
   *
   * @return Using displayType.
   */
  public static DisplayData getDisplayData(@Nullable ItemStack item) {
    try {
      /*
       * Nest structure as: armor-stand: - myStick: type: DEBUG_STICK lore: - mysterious item - gift
       * strict: true attribute: yaw: 180 pitch: 0 small: false item-slot: HELMET offset: x: 1 y:
       * 0.1 z: -1.1 pose-head: x: 1 pose-body: y: 0.2 pose-arm: left: z: 0.5 pose-leg: left: x:
       * 0.14 right: x: 0.3 y: 0.1 z: 0.3 - grasses: type: - GLASS - GRASS - GRASS_BLOCK real-item:
       * - 0: type: BOW lore: common mark
       */

      DisplayData def = new DisplayData(
          DisplayType.fromID(BaseConfig.displayTypeId), false,
          false);

      if (item != null) {
        YamlConfiguration conf =
            QuickShop.instance().getConfigurationManager().get(BaseConfig.class).conf();

        if (conf != null) {
          DisplayData dataArmourStand = DisplayData.matchData(conf, "shop.display-type-specifics.armor-stand", item, true);
          if (dataArmourStand != null && !dataArmourStand.needVaildate)
            return dataArmourStand.fixer
                ? (def.type == dataArmourStand.type ? dataArmourStand : def)
                : dataArmourStand;

          DisplayData dataDroppedItem = DisplayData.matchData(conf, "shop.display-type-specifics.dropped-item", item, false);
          if (dataDroppedItem != null)
            return dataDroppedItem.fixer
                ? (def.type == dataDroppedItem.type ? dataDroppedItem : def)
                : dataDroppedItem;
          else if (dataArmourStand != null)
            return dataArmourStand.fixer
                ? (def.type == dataArmourStand.type ? dataArmourStand : def)
                : dataArmourStand;
        }
      }

      return def;
    } catch (Throwable e) {
      e.printStackTrace();
      return new DisplayData(DisplayType.REALITEM, false, false);
    }
  }
  
  @Nullable
  public static DisplayData matchData(@NotNull ConfigurationSection conf, @NotNull String rootType,
      @NotNull ItemStack item, boolean armorStand) {
    List<?> specifics = conf.getList(rootType);
    // <Map>
    // Value: Specific Map

    if (specifics instanceof List) {
      return matchData0(specifics, item, armorStand);
    }
    Util.debugLog("Specifics Is Not A List: " + specifics);
    return null;
  }

  @Nullable
  private static DisplayData matchData0(@NotNull List<?> specifics, @NotNull ItemStack item,
      boolean armorStand) {
    DisplayData vaildateData = null;
    boolean needVaildate = false;
    for (Object o : specifics) {
      Util.debugLog("Specific: " + o);
      if (o instanceof Map) {
        Map<?, ?> specificMap = Map.class.cast(o);
        // <String, Map<String, ?>>
        // Key: Custom name of Specific
        // Value: Attribute Map

        for (Object o_ : specificMap.values()) {
          Map<?, ?> attrMap = Map.class.cast(o_);

          // Mode
          Object temp = attrMap.get("fixer");
          Object fixer = temp == null ? false : true;

          // Type matcher
          temp = attrMap.get("type");
          Object type = temp == null ? "TAG:ANY" : temp;

          if (!type.equals("TAG:ANY")) {
            if (type instanceof Collection) {
              Collection<?> c = Collection.class.cast(type);
              boolean containsType = c.contains(item.getType().name());

              if (c.contains("TYPE:!".concat(item.getType().name()))) {
                continue;
              }
              if (c.contains("TAG:BLOCK") && !containsType) {
                if (!item.getType().isBlock()) {
                  continue;
                }
                needVaildate = true;
                break;
              }
              if (c.contains("TAG:!BLOCK") && !containsType) {
                if (item.getType().isBlock()) {
                  continue;
                }
                needVaildate = true;
                break;
              }

              if (!containsType) {
                continue;
              }
            } else if (type instanceof String) {
              if (type.equals("TYPE:!".concat(item.getType().name()))) {
                continue;
              }
              if (type.equals("TAG:BLOCK")) {
                if (!item.getType().isBlock()) {
                  continue;
                }
                needVaildate = true;
                break;
              }
              if (type.equals("TAG:!BLOCK")) {
                if (item.getType().isBlock()) {
                  continue;
                }
                needVaildate = true;
                break;
              }

              if (!item.getType().name().equals((type))) {
                continue;
              }
            }
          } else {
            needVaildate = true;
          }

          // Custom Model Data matcher
          temp = attrMap.get("strict");
          boolean strict = temp == null ? false : (boolean) temp;

          temp = attrMap.get("model-data");
          if (temp != null) {
            if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
              if ((int) temp != item.getItemMeta().getCustomModelData()) {
                continue;
              }
            } else {
              continue;
            }
          } else {
            needVaildate = true;
          }

          // Lore matcher
          temp = attrMap.get("lore");
          Object lore = temp == null ? "" : temp;

          if (!lore.equals("") && item.hasItemMeta() && item.getItemMeta().hasLore()) {
            if (lore instanceof Collection) {
              Collection<?> specificLore = Collection.class.cast(lore);
              if (strict) {
                if (!specificLore.equals(item.getLore())) {
                  continue;
                }
              } else {
                boolean hasAny = false;

                LOOP_LORE: for (String s : item.getLore()) {
                  if (specificLore.contains(s)) {
                    hasAny = true;
                    break LOOP_LORE;
                  }
                }

                if (!hasAny) {
                  continue;
                } else {
                  needVaildate = true;
                }
              }
            } else if (lore instanceof String) {
              if (strict) {
                if (!item.getLore().equals(lore)) {
                  continue;
                }
              } else {
                if (!item.getLore().contains((lore))) {
                  continue;
                } else {
                  needVaildate = true;
                }
              }
            }
          } else {
            needVaildate = true;
          }

          DisplayData data =
              new DisplayData(armorStand ? DisplayType.ARMORSTAND : DisplayType.REALITEM,
                  needVaildate, (boolean) fixer);
          if (armorStand) {
            Map<?, ?> attributes = Map.class.cast(attrMap.get("attribute"));

            if (attributes instanceof Map) {
              for (DisplayAttribute attr : DisplayAttribute.values()) {
                String[] attrKeys = attr.name().split("_");
                String rootKey = attrKeys[0].toLowerCase(Locale.ROOT);

                if (attrKeys.length == 1) {
                  temp = Map.class.cast(attributes).get(rootKey);
                  data.attribute.put(attr, temp == null ? ObjectUtils.NULL : temp);
                } else {
                  // Nested Map
                  if ((temp = Map.class.cast(attributes).get(rootKey)) instanceof Map) {
                    String subKey = attrKeys[1].toLowerCase(Locale.ROOT);
                    Object value = Map.class.cast(temp).get(subKey);

                    data.attribute.put(attr, value == null ? ObjectUtils.NULL : value);
                  }
                }
              }
            }
          }

          if (needVaildate)
            vaildateData = data;
          else
            return data;
        }
      }
    }

    return vaildateData;
  }
}
