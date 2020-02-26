package org.maxgamer.quickshop.hologram;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang.ObjectUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.configuration.DisplayConfig;
import org.maxgamer.quickshop.shop.QuickShopDisplayData;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Maps;
import cc.bukkit.shop.hologram.DisplayAttribute;
import cc.bukkit.shop.hologram.DisplayData;
import cc.bukkit.shop.hologram.DisplayType;
import lombok.RequiredArgsConstructor;

public abstract class DisplayDataMatcher {
  /**
   * Matches from configuration and creates the display data for the given item
   * @param item The item to load display data for
   * @return The display data of that item
   */
  public static DisplayData create(@NotNull ItemStack item) {
    try {
      DisplayData def = new QuickShopDisplayData(DisplayType.valueOf(DisplayConfig.displayType), item);

      YamlConfiguration conf =
          QuickShop.instance().getConfigurationManager().get(DisplayConfig.class).conf();

      if (conf != null) {
        @Nullable DisplayDataMatchesResult dataArmourStand = DisplayDataMatcher.matchDataFor(conf,
            "settings.specifics.armor-stand", item, DisplayType.ARMOR_STAND);
        
        // Return if vaildate is not need (accurate matches)
        if (dataArmourStand != null && !dataArmourStand.needVaildate)
          return dataArmourStand.fixer
              ? (def.type() == dataArmourStand.data.type() ? dataArmourStand.data : def)
              : dataArmourStand.data;

        @Nullable DisplayDataMatchesResult dataDroppedItem = DisplayDataMatcher.matchDataFor(conf,
            "settings.specifics.dropped-item", item, DisplayType.DROPPED_ITEM);
        
        // Return or consider the vaildate-need data if not exists. 
        if (dataDroppedItem != null)
          return dataDroppedItem.fixer
              ? (def.type() == dataDroppedItem.data.type() ? dataDroppedItem.data : def)
              : dataDroppedItem.data;
        else if (dataArmourStand != null)
          return dataArmourStand.fixer
              ? (def.type() == dataArmourStand.data.type() ? dataArmourStand.data : def)
              : dataArmourStand.data;
      }

      return def;
    } catch (Throwable e) {
      e.printStackTrace();
      return new QuickShopDisplayData(DisplayType.DROPPED_ITEM, item);
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
    
    /**
     * Indicates whether this data is a display fixer.
     * A fixer menas only when the default display type matches,
     * this data will be applied.
     * @see BaseConfig#displayTypeId
     */
    @NotNull
    private final boolean fixer;
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
      Util.debug("Specifics Is Not A List: " + specifics);
    
    return null; // Handled by upstream method
  }

  @Nullable
  private static DisplayDataMatchesResult matchDataFor(
      @NotNull List<?> specifics,
      @NotNull ItemStack item,
      @NotNull DisplayType displayType) {
    
    boolean needVaildate = false;
    
    for (Object o : specifics) {
      Util.debug("Specific: " + o);
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

          Map<DisplayAttribute, Object> attrs = Maps.newEnumMap(DisplayAttribute.class);
          QuickShopDisplayData data = new QuickShopDisplayData(displayType, item, attrs);
          
          /*
           * Loads armor stand attributes
           */
          if (displayType == DisplayType.ARMOR_STAND) {
            Map<?, ?> attributes = Map.class.cast(attrMap.get("attribute"));

            if (attributes instanceof Map) {
              for (DisplayAttribute attr : DisplayAttribute.values()) {
                String[] attrKeys = attr.name().split("_");
                String rootKey = attrKeys[0].toLowerCase(Locale.ROOT);

                if (attrKeys.length == 1) {
                  temp = Map.class.cast(attributes).get(rootKey);
                  attrs.put(attr, temp == null ? ObjectUtils.NULL : temp);
                } else {
                  // Nested Map
                  if ((temp = Map.class.cast(attributes).get(rootKey)) instanceof Map) {
                    String subKey = attrKeys[1].toLowerCase(Locale.ROOT);
                    Object value = Map.class.cast(temp).get(subKey);

                    attrs.put(attr, value == null ? ObjectUtils.NULL : value);
                  }
                }
              }
            }
          }

          return new DisplayDataMatchesResult(data, needVaildate, fixer);
        }
      }
    }

    return null; // Handled by upstream method
  }
}
