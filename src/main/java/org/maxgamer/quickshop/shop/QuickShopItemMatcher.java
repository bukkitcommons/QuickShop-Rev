package org.maxgamer.quickshop.shop;

import java.util.Map;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.configuration.MatcherConfig;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Multimap;
import cc.bukkit.shop.misc.ItemStackMatcher;

/** A util allow quickshop check item matches easy and quick. */
public class QuickShopItemMatcher implements ItemStackMatcher {
    private boolean bukkit;
    
    public QuickShopItemMatcher() {
        this.bukkit = MatcherConfig.damage && MatcherConfig.repairCost && MatcherConfig.displayName && MatcherConfig.lore && MatcherConfig.enchant && MatcherConfig.matchesPotion && MatcherConfig.attribute && MatcherConfig.flag && MatcherConfig.customModelData;
    }
    
    @Override
    public boolean matches(@Nullable ItemStack requireStack, @Nullable ItemStack givenStack) {
        return matches(requireStack, givenStack, true);
    }
    
    /**
     * Compares two items to each other. Returns true if they match. Rewrite it to
     * use more faster hashCode.
     *
     * @param requireStack The first item stack
     * @param givenStack   The second item stack
     * @return true if the itemstacks match. (Material, durability, enchants, name)
     */
    @Override
    public boolean matches(@Nullable ItemStack requireStack, @Nullable ItemStack givenStack, boolean matchesAmount) {
        if (requireStack == givenStack)
            return true;
        
        if (requireStack == null || givenStack == null)
            return false;
        
        switch (MatcherConfig.matcherWorkMode) {
            case 1:
            case 2:
                return matchesAmount ? requireStack.equals(givenStack) : requireStack.isSimilar(givenStack);
            default:
                ;
        }
        
        if (requireStack.getType() != givenStack.getType()) {
            // Util.debugLog("Type not match.");
            return false;
        }
        
        if (matchesAmount && requireStack.getAmount() != givenStack.getAmount()) {
            Util.trace("Amount not match.");
            return false;
        }
        
        if (requireStack.hasItemMeta() != givenStack.hasItemMeta()) {
            Util.trace("Meta existence not match.");
            return false;
        }
        
        if (requireStack.hasItemMeta()) {
            return matches(requireStack.getItemMeta(), givenStack.getItemMeta());
        }
        
        return true;
    }
    
    /**
     * This method is almost as weird as notUncommon. Only return false if your
     * common internals are unequal. Checking your own internals is redundant if you
     * are not common, as notUncommon is meant for checking those 'not common'
     * variables.
     */
    @Override
    public boolean equalsCommon(ItemMeta meta, ItemMeta that) {
        return (MatcherConfig.displayName ? (meta.hasDisplayName() ? that.hasDisplayName() && meta.getDisplayName().equals(that.getDisplayName()) : !that.hasDisplayName()) : true)
                
                && (MatcherConfig.localizedName ? (meta.hasLocalizedName() ? that.hasLocalizedName() && meta.getLocalizedName().equals(that.getLocalizedName()) : !that.hasLocalizedName()) : true)
                
                && (MatcherConfig.enchant ? (meta.hasEnchants() ? that.hasEnchants() && meta.getEnchants().equals(that.getEnchants()) : !that.hasEnchants()) : true)
                
                && (MatcherConfig.lore ? (meta.hasLore() ? that.hasLore() && meta.getLore().equals(that.getLore()) : !that.hasLore()) : true)
                
                && (MatcherConfig.repairCost ? (meta instanceof org.bukkit.inventory.meta.Repairable ? that instanceof org.bukkit.inventory.meta.Repairable && ((org.bukkit.inventory.meta.Repairable) meta).getRepairCost() == ((org.bukkit.inventory.meta.Repairable) that).getRepairCost() : !(that instanceof org.bukkit.inventory.meta.Repairable)) : true)
                
                && (MatcherConfig.attribute ? (meta.hasAttributeModifiers() ? that.hasAttributeModifiers() && compareModifiers(meta.getAttributeModifiers(), that.getAttributeModifiers()) : !that.hasAttributeModifiers()) : true)
                
                && (comparePersistentDataContainer(meta, that))
                
                && (MatcherConfig.flag ? (meta.getItemFlags().equals(that.getItemFlags())) : true)
                
                && (MatcherConfig.unbreakable ? (meta.isUnbreakable() == that.isUnbreakable()) : true)
                
                // FIXME Missing NMS field: unhandledTags
                // FIXME Missing NMS field: version
                // FIXME Missing Paper API: destroyable & placeable
                && (MatcherConfig.damage ? (meta instanceof Damageable ? that instanceof Damageable && ((Damageable) meta).getDamage() == ((Damageable) that).getDamage() : !(that instanceof Damageable)) : true)
                
                && (compareCustomModelData(meta, that));
    }
    
    @Override
    public boolean compareCustomModelData(ItemMeta meta, ItemMeta that) {
        try {
            return MatcherConfig.customModelData ? (meta.hasCustomModelData() ? that.hasCustomModelData() && meta.getCustomModelData() == that.getCustomModelData() : !that.hasCustomModelData()) : true;
        } catch (Throwable t) {
            return true; // since 1.13
        }
    }
    
    @Override
    public boolean comparePersistentDataContainer(ItemMeta meta, ItemMeta that) {
        try {
            return MatcherConfig.customTags ? meta.getPersistentDataContainer().equals(that.getPersistentDataContainer()) : true;
        } catch (Throwable t) {
            return true; // since 1.14
        }
    }
    
    @Override
    public boolean matches(ItemMeta originMeta, ItemMeta testMeta) {
        return bukkit ? originMeta.equals(testMeta) : equalsCommon(originMeta, testMeta) && compareUncommon(originMeta, testMeta);
    }
    
    @Override
    public boolean compareUncommon(ItemMeta origin, ItemMeta test) {
        Class<? extends ItemMeta> clazz = origin.getClass();
        if (isCommon(origin.getClass())) // Skip common types
            return true;
        if (clazz != test.getClass()) // Not same meta type, item type was same though
            return false;
        
        try {
            boolean matches;
            
            matches = MatcherConfig.matchesBook ? matchesBook(clazz, origin, test) : true;
            matches = matches && MatcherConfig.matchesEnchantBook ? matchesEnchantmentStorage(clazz, origin, test) : true;
            matches = matches && MatcherConfig.matchesFirework ? matchesFirework(clazz, origin, test) : true;
            matches = matches && MatcherConfig.matchesFireworkCharge ? matchesFireworkEffect(clazz, origin, test) : true;
            matches = matches && MatcherConfig.matchesLeatherArmour ? matchesLeatherArmor(clazz, origin, test) : true;
            matches = matches && MatcherConfig.matchesMap ? matchesMap(clazz, origin, test) : true;
            matches = matches && MatcherConfig.matchesPotion ? matchesPotion(clazz, origin, test) : true;
            matches = matches && MatcherConfig.matchesSkull ? matchesSkull(clazz, origin, test) : true;
            matches = matches && MatcherConfig.matchesSpawnEgg ? matchesSpawnEgg(clazz, origin, test) : true;
            
            try {
                matches = matches && MatcherConfig.matchesBanner ? matchesBanner(clazz, origin, test) : true;
            } catch (Throwable t) {}
            
            try {
                matches = matches && MatcherConfig.matchesCrossbow ? matchesCrossbow(clazz, origin, test) : true;
            } catch (Throwable t) {}
            
            try {
                matches = matches && MatcherConfig.matchesKnowledgeBook ? matchesKnowledgeBook(clazz, origin, test) : true;
            } catch (Throwable t) {}
            
            try {
                matches = matches && MatcherConfig.matchesSuspiciousStew ? matchesSuspiciousStew(clazz, origin, test) : true;
            } catch (Throwable t) {}
            
            try {
                matches = matches && MatcherConfig.matchesTropicalFishBucket ? matchesTropicalFishBucket(clazz, origin, test) : true;
            } catch (Throwable t) {}
            
            return matches;
        } catch (Throwable t) {
            return origin.equals(test);
        }
    }
    
    public static boolean isCommon(Class<? extends ItemMeta> clazz) {
        try {
            return clazz == ItemMeta.class || clazz == org.bukkit.inventory.meta.BlockStateMeta.class || clazz == org.bukkit.inventory.meta.BlockDataMeta.class;
        } catch (Throwable t) {
            return false; // only throws on newer meta type, i.e. not ItemMeta
        }
    }
    
    private static boolean compareModifiers(Multimap<Attribute, AttributeModifier> first, Multimap<Attribute, AttributeModifier> second) {
        if (first == null || second == null)
            return false;
        
        for (Map.Entry<Attribute, AttributeModifier> entry : first.entries())
            if (!second.containsEntry(entry.getKey(), entry.getValue()))
                return false;
            
        for (Map.Entry<Attribute, AttributeModifier> entry : second.entries())
            if (!first.containsEntry(entry.getKey(), entry.getValue()))
                return false;
            
        return true;
    }
    
    @SuppressWarnings("deprecation")
    public static boolean matchesMap(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == MapMeta.class) {
            MapMeta meta = (MapMeta) originMeta, that = (MapMeta) testMeta;
            
            return MatcherConfig.matchesMapScaling ? meta.isScaling() == that.isScaling() : true
                    
                    && MatcherConfig.matchesMapId ? (meta.hasMapId() ? that.hasMapId() && meta.getMapId() == that.getMapId() : !that.hasMapId()) : true
                            
                            && MatcherConfig.matchesMapLocationName ? (meta.hasLocationName() ? that.hasLocationName() && meta.getLocationName().equals(that.getLocationName()) : !that.hasLocationName()) : true
                                    
                                    && MatcherConfig.matchesMapColour ? (meta.hasColor() ? that.hasColor() && meta.getColor().equals(that.getColor()) : !that.hasColor()) : true;
        } else {
            return true;
        }
    }
    
    public static boolean matchesPotion(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == org.bukkit.inventory.meta.PotionMeta.class) {
            org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) originMeta,
                    that = (org.bukkit.inventory.meta.PotionMeta) testMeta;
            
            return MatcherConfig.matchesPotion ? meta.getBasePotionData().equals(that.getBasePotionData()) : true
                    
                    && MatcherConfig.matchesPotionCustomEffects ? (meta.hasCustomEffects() ? that.hasCustomEffects() && meta.getCustomEffects().equals(that.getCustomEffects()) : !that.hasCustomEffects()) : true
                            
                            && MatcherConfig.matchesPotionColour ? (meta.hasColor() ? that.hasColor() && meta.getColor().equals(that.getColor()) : !that.hasColor()) : true;
        } else {
            return true;
        }
    }
    
    @SuppressWarnings("deprecation")
    public static boolean matchesSpawnEgg(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == org.bukkit.inventory.meta.SpawnEggMeta.class) {
            org.bukkit.inventory.meta.SpawnEggMeta that = (org.bukkit.inventory.meta.SpawnEggMeta) testMeta;
            // FIXME Missing NMS field: entityTag
            return that.getSpawnedType().equals(that.getSpawnedType());
        } else {
            return true;
        }
    }
    
    @SuppressWarnings("deprecation")
    public static boolean matchesSkull(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == org.bukkit.inventory.meta.SkullMeta.class) {
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) originMeta,
                    that = (org.bukkit.inventory.meta.SkullMeta) testMeta;
            
            return meta.hasOwner() ? that.hasOwner() && meta.getOwner().equals(that.getOwner()) : !that.hasOwner();
        } else {
            return true;
        }
    }
    
    public static boolean matchesBook(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == BookMeta.class) {
            BookMeta meta = (BookMeta) originMeta, that = (BookMeta) testMeta;
            
            return MatcherConfig.matchesBookTitle ? (meta.hasTitle() ? that.hasTitle() && meta.getTitle().equals(that.getTitle()) : !that.hasTitle()) : true
                    
                    && MatcherConfig.matchesBookAuthor ? (meta.hasAuthor() ? that.hasAuthor() && meta.getAuthor().equals(that.getAuthor()) : !that.hasAuthor()) : true
                            
                            && MatcherConfig.matchesBookPages ? (meta.hasPages() ? that.hasPages() && meta.getPages().equals(that.getPages()) : !that.hasPages()) : true
                                    
                                    && MatcherConfig.matchesBookGeneration ? (meta.hasGeneration() ? that.hasGeneration() && meta.getGeneration().equals(that.getGeneration()) : !that.hasGeneration()) : true;
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
            org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) originMeta,
                    that = (org.bukkit.inventory.meta.EnchantmentStorageMeta) testMeta;
            
            return meta.hasStoredEnchants() ? that.hasStoredEnchants() && meta.getEnchants().equals(that.getEnchants()) : !that.hasStoredEnchants();
        } else {
            return true;
        }
    }
    
    public static boolean matchesFireworkEffect(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == org.bukkit.inventory.meta.FireworkEffectMeta.class) {
            org.bukkit.inventory.meta.FireworkEffectMeta meta = (org.bukkit.inventory.meta.FireworkEffectMeta) originMeta,
                    that = (org.bukkit.inventory.meta.FireworkEffectMeta) testMeta;
            
            return meta.hasEffect() ? that.hasEffect() && meta.getEffect().equals(that.getEffect()) : !that.hasEffect();
        } else {
            return true;
        }
    }
    
    public static boolean matchesFirework(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == org.bukkit.inventory.meta.FireworkMeta.class) {
            org.bukkit.inventory.meta.FireworkMeta meta = (org.bukkit.inventory.meta.FireworkMeta) originMeta,
                    that = (org.bukkit.inventory.meta.FireworkMeta) testMeta;
            
            return MatcherConfig.matchesFireworkPower ? meta.getPower() == that.getPower() : true &&
                    
                    MatcherConfig.matchesFireworkEffects ? (meta.hasEffects() ? that.hasEffects() && meta.getEffects().equals(that.getEffects()) : !that.hasEffects()) : true;
        } else {
            return true;
        }
    }
    
    @SuppressWarnings("deprecation")
    public static boolean matchesBanner(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == org.bukkit.inventory.meta.BannerMeta.class) {
            org.bukkit.inventory.meta.BannerMeta meta = (org.bukkit.inventory.meta.BannerMeta) originMeta,
                    that = (org.bukkit.inventory.meta.BannerMeta) testMeta;
            
            return (MatcherConfig.matchesBannerPattern ? (meta.getPatterns().equals(that.getPatterns())) : true) || (MatcherConfig.matchesBannerBaseColour ? (meta.getBaseColor().equals(that.getBaseColor())) : true);
        } else {
            return true;
        }
    }
    
    public static boolean matchesKnowledgeBook(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == org.bukkit.inventory.meta.KnowledgeBookMeta.class) {
            org.bukkit.inventory.meta.KnowledgeBookMeta meta = (org.bukkit.inventory.meta.KnowledgeBookMeta) originMeta,
                    that = (org.bukkit.inventory.meta.KnowledgeBookMeta) testMeta;
            
            return meta.hasRecipes() ? that.hasRecipes() && meta.getRecipes().equals(that.getRecipes()) : !that.hasRecipes();
        } else {
            return true;
        }
    }
    
    public static boolean matchesSuspiciousStew(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == org.bukkit.inventory.meta.SuspiciousStewMeta.class) {
            org.bukkit.inventory.meta.SuspiciousStewMeta meta = (org.bukkit.inventory.meta.SuspiciousStewMeta) originMeta,
                    that = (org.bukkit.inventory.meta.SuspiciousStewMeta) testMeta;
            
            return (meta.hasCustomEffects() ? that.hasCustomEffects() && meta.getCustomEffects().equals(that.getCustomEffects()) : !that.hasCustomEffects());
        } else {
            return true;
        }
    }
    
    public static boolean matchesTropicalFishBucket(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == org.bukkit.inventory.meta.TropicalFishBucketMeta.class) {
            org.bukkit.inventory.meta.TropicalFishBucketMeta meta = (org.bukkit.inventory.meta.TropicalFishBucketMeta) originMeta,
                    that = (org.bukkit.inventory.meta.TropicalFishBucketMeta) testMeta;
            
            return (meta.hasVariant() ? that.hasVariant() && (MatcherConfig.matchesTropicalFishBucketPattern ? meta.getPattern().equals(that.getPattern()) : true &&
                    
                    MatcherConfig.matchesTropicalFishBucketPatternColour ? meta.getPatternColor().equals(that.getPatternColor()) : true &&
                            
                            MatcherConfig.matchesTropicalFishBucketBodyColour ? meta.getBodyColor().equals(that.getBodyColor()) : true
            
            ) : !that.hasVariant());
        } else {
            return true;
        }
    }
    
    public static boolean matchesCrossbow(Class<? extends ItemMeta> clazz, ItemMeta originMeta, ItemMeta testMeta) {
        if (clazz == org.bukkit.inventory.meta.CrossbowMeta.class) {
            org.bukkit.inventory.meta.CrossbowMeta meta = (org.bukkit.inventory.meta.CrossbowMeta) originMeta,
                    that = (org.bukkit.inventory.meta.CrossbowMeta) testMeta;
            // FIXME Missing NMS field: charged
            return meta.hasChargedProjectiles() ? that.hasChargedProjectiles() && meta.getChargedProjectiles().equals(that.getChargedProjectiles()) : !that.hasChargedProjectiles();
        } else {
            return true;
        }
    }
}
