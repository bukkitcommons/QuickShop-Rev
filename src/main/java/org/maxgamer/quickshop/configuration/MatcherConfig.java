package org.maxgamer.quickshop.configuration;

import cc.bukkit.shop.configuration.Configuration;
import cc.bukkit.shop.configuration.Node;

@Configuration("configs/matchers.yml")
public class MatcherConfig {
  @Node(value = "settings.matcher")
  public static int matcherWorkMode = 0;
  
  @Node(value = "settings.damage")
  public static boolean damage = true;

  @Node(value = "settings.attribute")
  public static boolean attribute = true;

  @Node(value = "settings.custom-model-data")
  public static boolean customModelData = true;

  @Node(value = "settings.display-name")
  public static boolean displayName = true;

  @Node(value = "settings.enchant")
  public static boolean enchant = true;
  
  @Node(value = "settings.localized-name")
  public static boolean localizedName = true;

  @Node(value = "settings.flag")
  public static boolean flag = true;
  
  @Node(value = "settings.unbreakable")
  public static boolean unbreakable = true;
  
  @Node(value = "settings.custom-tags")
  public static boolean customTags = true;

  @Node(value = "settings.lore")
  public static boolean lore = true;

  @Node(value = "settings.type.potion.enable")
  public static boolean matchesPotion = true;
  
  @Node(value = "settings.type.potion.attributes.data")
  public static boolean matchesPotionData = true;
  
  @Node(value = "settings.type.potion.attributes.custom-effects")
  public static boolean matchesPotionCustomEffects = true;
  
  @Node(value = "settings.type.potion.attributes.colour")
  public static boolean matchesPotionColour = true;
  
  @Node(value = "settings.type.map.enable")
  public static boolean matchesMap = true;
  
  @Node(value = "settings.type.map.attributes.scaling")
  public static boolean matchesMapScaling = true;
  
  @Node(value = "settings.type.map.attributes.id")
  public static boolean matchesMapId = true;
  
  @Node(value = "settings.type.map.attributes.location-name")
  public static boolean matchesMapLocationName = true;
  
  @Node(value = "settings.type.map.attributes.colour")
  public static boolean matchesMapColour = true;
  
  @Node(value = "settings.type.banner.enable")
  public static boolean matchesBanner = true;
  
  @Node(value = "settings.type.banner.attributes.pattern")
  public static boolean matchesBannerPattern = true;
  
  @Node(value = "settings.type.banner.attributes.base-colour")
  public static boolean matchesBannerBaseColour = true;
  
  @Node(value = "settings.type.skull.enable")
  public static boolean matchesSkull = true;
  
  @Node(value = "settings.type.spawn-egg.enable")
  public static boolean matchesSpawnEgg = true;
  
  @Node(value = "settings.type.leather-armour.enable")
  public static boolean matchesLeatherArmour = true;
  
  @Node(value = "settings.type.firework.enable")
  public static boolean matchesFirework = true;
  
  @Node(value = "settings.type.firework.attributes.power")
  public static boolean matchesFireworkPower = true;
  
  @Node(value = "settings.type.firework.attributes.effects")
  public static boolean matchesFireworkEffects = true;
  
  @Node(value = "settings.type.firework-charge.enable")
  public static boolean matchesFireworkCharge = true;
  
  @Node(value = "settings.type.enchant-book.enable")
  public static boolean matchesEnchantBook = true;
  
  @Node(value = "settings.type.book.enable")
  public static boolean matchesBook = true;
  
  @Node(value = "settings.type.book.attributes.title")
  public static boolean matchesBookTitle = true;
  
  @Node(value = "settings.type.book.attributes.author")
  public static boolean matchesBookAuthor = true;
  
  @Node(value = "settings.type.book.attributes.pages")
  public static boolean matchesBookPages = true;
  
  @Node(value = "settings.type.book.attributes.generation")
  public static boolean matchesBookGeneration = true;
  
  @Node(value = "settings.type.crossbow.enable")
  public static boolean matchesCrossbow = true;
  
  @Node(value = "settings.type.knowledge-book.enable")
  public static boolean matchesKnowledgeBook = true;
  
  @Node(value = "settings.type.suspicious-stew.enable")
  public static boolean matchesSuspiciousStew = true;
  
  @Node(value = "settings.type.tropical-fish-bucket.enable")
  public static boolean matchesTropicalFishBucket = true;
  
  @Node(value = "settings.type.tropical-fish-bucket.attributes.pattern")
  public static boolean matchesTropicalFishBucketPattern = true;
  
  @Node(value = "settings.type.tropical-fish-bucket.attributes.pattern-colour")
  public static boolean matchesTropicalFishBucketPatternColour = true;
  
  @Node(value = "settings.type.tropical-fish-bucket.attributes.body-colour")
  public static boolean matchesTropicalFishBucketBodyColour = true;

  @Node(value = "settings.type.repair-cost.enable")
  public static boolean repairCost = true;
}
