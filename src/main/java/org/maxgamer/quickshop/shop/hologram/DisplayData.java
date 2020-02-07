package org.maxgamer.quickshop.shop.hologram;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang.ObjectUtils;
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
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@AllArgsConstructor
public class DisplayData {
  @Getter
  private final DisplayType type;
  
  /**
   * Stores specificed attribute data
   * @see DisplayAttribute
   */
  private final Map<DisplayAttribute, Object> attribute = Maps.newEnumMap(DisplayAttribute.class);
  
  /**
   * Indicates whether this data is a display fixer.
   * A fixer menas only when the default display type matches,
   * this data will be applied.
   * @see BaseConfig#displayTypeId
   */
  private final boolean fixer;
  
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
      ShopLogger.instance()
          .warning("Error when processing attribute for " + attr.name() + " with unexpected value "
              + value.toString() + ", please check your config before reporting!");
      t.printStackTrace();
      return defaultValue;
    }
  }
  
  /**
   * Obtain an default display data with default display type. 
   * @return The empty data
   */
  public static DisplayData create() {
    return new DisplayData(DisplayType.fromID(BaseConfig.displayTypeId), false);
  }
  
  /**
   * Matches from configuration and creates the display data for the given item
   * @param item The item to load display data for
   * @return The display data of that item
   */
  public static DisplayData create(@NotNull ItemStack item) {
    try {
      DisplayData def = new DisplayData(DisplayType.fromID(BaseConfig.displayTypeId), false);

      YamlConfiguration conf =
          QuickShop.instance().getConfigurationManager().get(BaseConfig.class).conf();

      if (conf != null) {
        @Nullable DisplayDataMatchesResult dataArmourStand = DisplayData.matchDataFor(conf,
            "shop.display-type-specifics.armor-stand", item, DisplayType.ARMORSTAND);
        
        // Return if vaildate is not need (accurate matches)
        if (dataArmourStand != null && !dataArmourStand.needVaildate)
          return dataArmourStand.data.fixer
              ? (def.type == dataArmourStand.data.type ? dataArmourStand.data : def)
              : dataArmourStand.data;

        @Nullable DisplayDataMatchesResult dataDroppedItem = DisplayData.matchDataFor(conf,
            "shop.display-type-specifics.dropped-item", item, DisplayType.REALITEM);
        
        // Return or consider the vaildate-need data if not exists. 
        if (dataDroppedItem != null)
          return dataDroppedItem.data.fixer
              ? (def.type == dataDroppedItem.data.type ? dataDroppedItem.data : def)
              : dataDroppedItem.data;
        else if (dataArmourStand != null)
          return dataArmourStand.data.fixer
              ? (def.type == dataArmourStand.data.type ? dataArmourStand.data : def)
              : dataArmourStand.data;
      }

      return def;
    } catch (Throwable e) {
      e.printStackTrace();
      return new DisplayData(DisplayType.REALITEM, false);
    }
  }
  
  /*
   * Data Matchers
   * 
   * The following methods are the matchers to match and load display data of that item,
   * after that, a DisplayData object will be created with its data wrapped in.
   */
  @RequiredArgsConstructor
  private static class DisplayDataMatchesResult {
    @NotNull
    private final DisplayData data;
    
    @NotNull
    private final boolean needVaildate;
  }
  
  @Nullable
  private static DisplayDataMatchesResult matchDataFor(
      @NotNull ConfigurationSection conf,
      @NotNull String rootType,
      @NotNull ItemStack item,
      @NotNull DisplayType displayType) {
    
    List<?> specifics = conf.getList(rootType);
    // <Map>
    // Value: Specific Map

    if (specifics instanceof List)
      return matchDataFor(specifics, item, displayType);
    else if (specifics != null)
      Util.debugLog("Specifics Is Not A List: " + specifics);
    
    return null; // Handled by upstream method
  }

  @Nullable
  private static DisplayDataMatchesResult matchDataFor(
      @NotNull List<?> specifics,
      @NotNull ItemStack item,
      @NotNull DisplayType displayType) {
    
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
          boolean fixer = temp == null ? false : (boolean) temp;

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

          DisplayData data = new DisplayData(displayType, fixer);
          
          /*
           * Loads armor stand attributes
           */
          if (displayType == DisplayType.ARMORSTAND) {
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

          return new DisplayDataMatchesResult(data, needVaildate);
        }
      }
    }

    return null; // Handled by upstream method
  }
}
