package org.maxgamer.quickshop.utils;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import com.google.common.collect.Multimap;

/** A util allow quickshop check item matches easy and quick. */
public class ItemMatcher {
  private ItemMetaMatcher itemMetaMatcher;

  public ItemMatcher() {
    itemMetaMatcher = new ItemMetaMatcher();
  }

  /**
   * Compares two items to each other. Returns true if they match. Rewrite it to use more faster
   * hashCode.
   *
   * @param requireStack The first item stack
   * @param givenStack The second item stack
   * @return true if the itemstacks match. (Material, durability, enchants, name)
   */
  public boolean matches(@Nullable ItemStack requireStack, @Nullable ItemStack givenStack) {
    if (requireStack == givenStack) {
      return true; // Referring to the same thing, or both are null.
    }

    if (requireStack == null || givenStack == null) {
      Util.debugLog("Match failed: A stack is null: " + "requireStack[" + requireStack
          + "] givenStack[" + givenStack + "]");
      return false; // One of them is null (Can't be both, see above)
    }

    switch (BaseConfig.matcherWorkMode) {
      case 1:
      case 2:
        Bukkit.getItemFactory().equals(null, null);
        return requireStack.isSimilar(givenStack);
      case 0:
      default:;
    }

    if (requireStack.getType() != givenStack.getType()) {
      Util.debugLog("Type not match.");
      return false;
    }
    
    if (requireStack.hasItemMeta() != givenStack.hasItemMeta()) {
      Util.debugLog("Meta existence not match.");
      return false;
    }

    return requireStack.hasItemMeta() ?
        itemMetaMatcher.matches(requireStack.getItemMeta(), givenStack.getItemMeta()) : true;
  }
}


class ItemMetaMatcher {
  private boolean repaircost;
  private boolean attributes;
  private boolean custommodeldata;
  private boolean damage;
  private boolean displayname;
  private boolean enchs;
  private boolean itemflags;
  private boolean lores;
  private boolean potions;
  ///
  private boolean bukkit;

  public ItemMetaMatcher() {
    this.damage = BaseConfig.damage;
    this.repaircost = BaseConfig.repairCost;
    this.displayname = BaseConfig.displayName;
    this.lores = BaseConfig.lore;
    this.enchs = BaseConfig.enchant;
    this.potions = BaseConfig.potion;
    this.attributes = BaseConfig.attribute;
    this.itemflags = BaseConfig.flag;
    this.custommodeldata = BaseConfig.customModelData;

    this.bukkit = damage && repaircost && displayname && lores && enchs && potions && attributes
        && itemflags && custommodeldata;
  }

  /**
   * This method is almost as weird as notUncommon.
   * Only return false if your common internals are unequal.
   * Checking your own internals is redundant if you are not common, as notUncommon is meant for checking those 'not common' variables.
   */
  public boolean equalsCommon(ItemMeta meta, ItemMeta that) {
    return
        displayname ?
            ((meta.hasDisplayName() ?
                that.hasDisplayName() &&
                meta.getDisplayName().equals(that.getDisplayName()) :
                  !that.hasDisplayName()))
        : true

        && (meta.hasLocalizedName() ?
            that.hasLocalizedName() &&
            meta.getLocalizedName().equals(that.getLocalizedName()) :
              !that.hasLocalizedName())

        && enchs ?
            (meta.hasEnchants() ?
                that.hasEnchants() &&
                meta.getEnchants().equals(that.getEnchants()) :
                  !that.hasEnchants())
           : true

        && lores ?
            (meta.hasLore() ?
                that.hasLore() &&
                meta.getLore().equals(that.getLore()) :
                  !that.hasLore())
           : true

        && repaircost ?
            (meta instanceof org.bukkit.inventory.meta.Repairable ?
                that instanceof org.bukkit.inventory.meta.Repairable &&
                ((org.bukkit.inventory.meta.Repairable) meta).getRepairCost() == ((org.bukkit.inventory.meta.Repairable) that).getRepairCost() :
                  !(that instanceof org.bukkit.inventory.meta.Repairable))
           : true

        && attributes ?
            (meta.hasAttributeModifiers() ?
                that.hasAttributeModifiers() &&
                compareModifiers(meta.getAttributeModifiers(), that.getAttributeModifiers()) :
                  !that.hasAttributeModifiers())
           : true

        && (meta.getPersistentDataContainer().equals(that.getPersistentDataContainer()))

        && itemflags ? (meta.getItemFlags().equals(that.getItemFlags())) : true

        && (meta.isUnbreakable() == that.isUnbreakable())

        // FIXME Missing NMS field: unhandledTags
        // FIXME Missing NMS field: version
        // FIXME Missing Paper API: destroyable & placeable
        && damage ?
            (meta instanceof Damageable ?
                that instanceof Damageable &&
                ((Damageable) meta).getDamage() == ((Damageable) that).getDamage() :
                  !(that instanceof Damageable))
           : true
        
        && custommodeldata ?
            (meta.hasCustomModelData() ?
                that.hasCustomModelData() &&
                meta.getCustomModelData() == that.getCustomModelData() :
                  !that.hasCustomModelData())
           : true;
  }
  
  public boolean matches(ItemMeta originMeta, ItemMeta testMeta) {
    return bukkit ?
        originMeta.equals(testMeta) :
        equalsCommon(originMeta, testMeta) && compareUncommon(originMeta, testMeta);
  }

  public boolean compareUncommon(ItemMeta originMeta, ItemMeta testMeta) {
    Class<? extends ItemMeta> clazz = originMeta.getClass();
    if (isCommon(clazz))
      return true;
    if (clazz != testMeta.getClass())
      return false;
    
    try {
      boolean matches;
      
      matches = matchesBook(clazz, originMeta, testMeta);
      matches = matches && matchesEnchantmentStorage(clazz, originMeta, testMeta);
      matches = matches && matchesFirework(clazz, originMeta, testMeta);
      matches = matches && matchesFireworkEffect(clazz, originMeta, testMeta);
      matches = matches && matchesLeatherArmor(clazz, originMeta, testMeta);
      matches = matches && matchesMap(clazz, originMeta, testMeta);
      matches = matches && potions ? matchesPotion(clazz, originMeta, testMeta) : true;
      matches = matches && matchesSkull(clazz, originMeta, testMeta);
      matches = matches && matchesSpawnEgg(clazz, originMeta, testMeta);
      
      try {
        matches = matches && matchesBanner(clazz, originMeta, testMeta);
      } catch (Throwable t) {}
      
      try {
        matches = matches && matchesCrossbow(clazz, originMeta, testMeta);
      } catch (Throwable t) {}
      
      try {
        matches = matches && matchesKnowledgeBook(clazz, originMeta, testMeta);
      } catch (Throwable t) {}
      
      try {
        matches = matches && matchesSuspiciousStew(clazz, originMeta, testMeta);
      } catch (Throwable t) {}
      
      try {
        matches = matches && matchesTropicalFishBucket(clazz, originMeta, testMeta);
      } catch (Throwable t) {}
      
      Util.debugLog("Found unknown uncommon meta type: " + clazz.getName());
      return originMeta.equals(testMeta);
    } catch (Throwable t) {
      return originMeta.equals(testMeta);
    }
  }

  public static boolean isCommon(Class<? extends ItemMeta> clazz) {
    try {
      return
          clazz == ItemMeta.class ||
          clazz == org.bukkit.inventory.meta.BlockStateMeta.class ||
          clazz == org.bukkit.inventory.meta.BlockDataMeta.class;
    } catch (Throwable t) {
      return false;
    }
  }

  private static boolean compareModifiers(Multimap<Attribute, AttributeModifier> first, Multimap<Attribute, AttributeModifier> second) {
    if (first == null || second == null) {
      return false;
    }
    for (Map.Entry<Attribute, AttributeModifier> entry : first.entries()) {
      if (!second.containsEntry(entry.getKey(), entry.getValue())) {
        return false;
      }
    }
    for (Map.Entry<Attribute, AttributeModifier> entry : second.entries()) {
      if (!first.containsEntry(entry.getKey(), entry.getValue())) {
        return false;
      }
    }
    return true;
  }
  
  public static boolean matchesMap(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == MapMeta.class) {
      MapMeta meta = (MapMeta) originMeta, that = (MapMeta) testMeta;
      
      return meta.isScaling() == that.isScaling()
          && (meta.hasMapId() ? that.hasMapId() && meta.getMapId() == that.getMapId() : !that.hasMapId())
          && (meta.hasLocationName() ? that.hasLocationName() && meta.getLocationName().equals(that.getLocationName()) : !that.hasLocationName())
          && (meta.hasColor() ? that.hasColor() && meta.getColor().equals(that.getColor()) : !that.hasColor());
    } else {
      return true;
    }
  }
  
  public static boolean matchesPotion(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.PotionMeta.class) {
      org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) originMeta, that = (org.bukkit.inventory.meta.PotionMeta) testMeta;
      
      return meta.getBasePotionData().equals(that.getBasePotionData())
          && (meta.hasCustomEffects() ? that.hasCustomEffects() && meta.getCustomEffects().equals(that.getCustomEffects()) : !that.hasCustomEffects())
          && (meta.hasColor() ? that.hasColor() && meta.getColor().equals(that.getColor()) : !that.hasColor());
    } else {
      return true;
    }
  }
  
  public static boolean matchesSpawnEgg(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.SpawnEggMeta.class) {
      org.bukkit.inventory.meta.SpawnEggMeta that = (org.bukkit.inventory.meta.SpawnEggMeta) testMeta;
      // FIXME Missing NMS field: entityTag
      return that.getSpawnedType().equals(that.getSpawnedType());
    } else {
      return true;
    }
  }
  
  public static boolean matchesSkull(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.SkullMeta.class) {
      org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) originMeta, that = (org.bukkit.inventory.meta.SkullMeta) testMeta;
      
      return meta.hasOwner() ? that.hasOwner() && meta.getOwner().equals(that.getOwner()) : !that.hasOwner();
    } else {
      return true;
    }
  }
  
  public static boolean matchesBook(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == BookMeta.class) {
      BookMeta meta = (BookMeta) originMeta, that = (BookMeta) testMeta;
      
      return
          (meta.hasTitle() ? that.hasTitle() && meta.getTitle().equals(that.getTitle()) : !that.hasTitle())
          && (meta.hasAuthor() ? that.hasAuthor() && meta.getAuthor().equals(that.getAuthor()) : !that.hasAuthor())
          && (meta.hasPages() ? that.hasPages() && meta.getPages().equals(that.getPages()) : !that.hasPages())
          && (meta.hasGeneration() ? that.hasGeneration() && meta.getGeneration().equals(that.getGeneration()) : !that.hasGeneration());
    } else {
      return true;
    }
  }
  
  public static boolean matchesLeatherArmor(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.LeatherArmorMeta.class) {
      return ((org.bukkit.inventory.meta.LeatherArmorMeta) originMeta).getColor().equals(((org.bukkit.inventory.meta.LeatherArmorMeta) testMeta).getColor());
    } else {
      return true;
    }
  }
  
  public static boolean matchesEnchantmentStorage(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.EnchantmentStorageMeta.class) {
      org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) originMeta, that = (org.bukkit.inventory.meta.EnchantmentStorageMeta) testMeta;
      
      return meta.hasStoredEnchants() ?
          that.hasStoredEnchants() && meta.getEnchants().equals(that.getEnchants()) : !that.hasStoredEnchants();
    } else {
      return true;
    }
  }
  
  public static boolean matchesFireworkEffect(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.FireworkEffectMeta.class) {
      org.bukkit.inventory.meta.FireworkEffectMeta meta = (org.bukkit.inventory.meta.FireworkEffectMeta) originMeta, that = (org.bukkit.inventory.meta.FireworkEffectMeta) testMeta;
      
      return meta.hasEffect() ?
          that.hasEffect() && meta.getEffect().equals(that.getEffect()) : !that.hasEffect();
    } else {
      return true;
    }
  }
  
  public static boolean matchesFirework(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.FireworkMeta.class) {
      org.bukkit.inventory.meta.FireworkMeta meta = (org.bukkit.inventory.meta.FireworkMeta) originMeta, that = (org.bukkit.inventory.meta.FireworkMeta) testMeta;
      
      return meta.getPower() == that.getPower() &&
          (meta.hasEffects() ? that.hasEffects() && meta.getEffects().equals(that.getEffects()) : !that.hasEffects());
    } else {
      return true;
    }
  }
  
  public static boolean matchesBanner(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.BannerMeta.class) {
      org.bukkit.inventory.meta.BannerMeta meta = (org.bukkit.inventory.meta.BannerMeta) originMeta, that = (org.bukkit.inventory.meta.BannerMeta) testMeta;
      
      return
          (meta.getPatterns().equals(that.getPatterns())) ||
          (meta.getBaseColor().equals(that.getBaseColor()));
    } else {
      return true;
    }
  }
  
  public static boolean matchesKnowledgeBook(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.KnowledgeBookMeta.class) {
      org.bukkit.inventory.meta.KnowledgeBookMeta meta = (org.bukkit.inventory.meta.KnowledgeBookMeta) originMeta, that = (org.bukkit.inventory.meta.KnowledgeBookMeta) testMeta;
      
      return meta.hasRecipes() ? that.hasRecipes() && meta.getRecipes().equals(that.getRecipes()) : !that.hasRecipes();
    } else {
      return true;
    }
  }
  
  public static boolean matchesSuspiciousStew(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.SuspiciousStewMeta.class) {
      org.bukkit.inventory.meta.SuspiciousStewMeta meta = (org.bukkit.inventory.meta.SuspiciousStewMeta) originMeta, that = (org.bukkit.inventory.meta.SuspiciousStewMeta) testMeta;
      
      return (meta.hasCustomEffects() ?
          that.hasCustomEffects() && meta.getCustomEffects().equals(that.getCustomEffects()) : !that.hasCustomEffects());
    } else {
      return true;
    }
  }
  
  public static boolean matchesTropicalFishBucket(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.TropicalFishBucketMeta.class) {
      org.bukkit.inventory.meta.TropicalFishBucketMeta meta = (org.bukkit.inventory.meta.TropicalFishBucketMeta) originMeta, that = (org.bukkit.inventory.meta.TropicalFishBucketMeta) testMeta;
      
      return
          (meta.hasVariant() ? that.hasVariant() && (
              meta.getPattern().equals(that.getPattern()) &&
              meta.getPatternColor().equals(that.getPatternColor()) && meta.getBodyColor().equals(that.getBodyColor())
          ) : !that.hasVariant());
    } else {
      return true;
    }
  }
  
  public static boolean matchesCrossbow(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
    if (clazz == org.bukkit.inventory.meta.CrossbowMeta.class) {
      org.bukkit.inventory.meta.CrossbowMeta meta = (org.bukkit.inventory.meta.CrossbowMeta) originMeta, that = (org.bukkit.inventory.meta.CrossbowMeta) testMeta;
      // FIXME Missing NMS field: charged
      return meta.hasChargedProjectiles() ?
          that.hasChargedProjectiles() && meta.getChargedProjectiles().equals(that.getChargedProjectiles()) : !that.hasChargedProjectiles();
    } else {
      return true;
    }
  }
}
