package org.maxgamer.quickshop.configuration.impl;

import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.maxgamer.quickshop.configuration.annotation.Configuration;
import org.maxgamer.quickshop.configuration.annotation.Node;
import com.google.common.collect.Lists;

@Configuration("base-config.yml")
public class BaseConfig {
  /** Default database prefix, can overwrite by config */
  @Node("settings.database.prefix")
  public static String databasePrefix = "";

  @Node("settings.database.queue")
  public static boolean databaseQueue = true;

  /** Whether we should use display items or not */
  @Node("settings.effects.display.enable-hologram")
  public static boolean displayItems = true;

  @Node(value = "settings.plugin.developer-mode", rewrite = true)
  public static boolean developerMode = false;

  @Node(value = "settings.find-distance")
  public static int findDistance = 45;

  @Node(value = "settings.plugin.language")
  public static String language = "default";

  @Node(value = "settings.plugin.logger.use-log4j")
  public static boolean useLog4j = true;
  
  @Node(value = "settings.plugin.logger.log-actions")
  public static boolean logActions = true;

  @Node(value = "settings.plugin.logger.debug", rewrite = true)
  public static boolean debugLogger = false;

  @Node(value = "settings.effects.sound.on-tab-complete")
  public static boolean tabCompleteSound = true;

  @Node(value = "settings.effects.currency-symbol")
  public static String currencySymbol = "$";

  @Node(value = "settings.allow-loan")
  public static boolean allowLoan = false;

  @Node(value = "settings.effects.display.type")
  public static int displayTypeId = 0;

  @Node(value = "settings.tax.account")
  public static String taxAccount = "";
  
  @Node(value = "settings.tax.rate")
  public static double taxRate = 0.01;

  @Node(value = "settings.matcher.item.damage")
  public static boolean damage = true;

  @Node(value = "settings.matcher.item.attribute")
  public static boolean attribute = true;

  @Node(value = "settings.matcher.item.custom-model-data")
  public static boolean customModelData = false;

  @Node(value = "settings.matcher.item.display-name")
  public static boolean displayName = true;

  @Node(value = "settings.matcher.item.enchant")
  public static boolean enchant = true;

  @Node(value = "settings.matcher.item.flag")
  public static boolean flag = true;

  @Node(value = "settings.matcher.item.lore")
  public static boolean lore = true;

  @Node(value = "settings.matcher.item.potion")
  public static boolean potion = true;

  @Node(value = "settings.matcher.item.repair-cost")
  public static boolean repairCost = true;

  @Node(value = "settings.enable-protection")
  public static boolean enableProtection = true;

  @Node(value = "settings.plugin.enable-updater")
  public static boolean enableUpdater = true;

  @Node(value = "settings.eco.price.mod-fee")
  public static double priceModFee = -1;

  @Node(value = "settings.eco.price.minimum")
  public static double minimumPrice = -1;

  @Node(value = "settings.eco.price.maximum")
  public static double maximumPrice = -1;

  @Node(value = "settings.eco.price.integer-only")
  public static boolean integerPriceOnly = false;

  @Node(value = "settings.eco.price.allow-free-shops")
  public static boolean allowFreeShops = true;

  @Node(value = "settings.plugin.enable-error-reporter")
  public static boolean eanbleErrorReporter = true;

  @Node(value = "settings.effects.display.despawner.enable")
  public static boolean enableDespawner = false;

  @Node(value = "settings.effects.display.despawner.range")
  public static int despawnerRange = 20;

  @Node(value = "settings.effects.sound.on-click")
  public static boolean clickSound = true;

  @Node(value = "settings.matcher.work-mode")
  public static int matcherWorkMode = 0;

  @Node(value = "settings.eco.system-type")
  public static int economyType = 0;

  @Node(value = "settings.eco.mixed.command.deposit")
  public static String mixedDepositCommand = "eco give {0} {1}";

  @Node(value = "settings.eco.mixed.command.withdraw")
  public static String mixedWithdrawCommand = "eco take {0} {1}";

  @Node(value = "settings.eco.price.use-decimal-format")
  public static boolean decimalFormatPrice = true;

  @Node(value = "settings.ignore-chat-cancelling")
  public static boolean ignoreChatCancelling = false;

  @Node(value = "settings.safety.enhanced-display-protection")
  public static boolean enhancedDisplayProtection = false;
  
  @Node(value = "settings.safety.enhanced-shop-protection")
  public static boolean enhancedShopProtection = false;

  @Node(value = "settings.safety.enable-alert")
  public static boolean enableAlert = false;

  @Node(value = "settings.safety.explosion-protection")
  public static boolean explosionProtection = true;

  @Node(value = "settings.safety.entity-protection")
  public static boolean entityProtection = true;

  @Node(value = "settings.lockette.enable")
  public static boolean locketteEnable = true;

  @Node(value = "settings.lockette.private-text")
  public static String lockettePrivateText = "[Private]";

  @Node(value = "settings.lockette.more-users-text")
  public static String locketteMoreUsersText = "[More Users]";

  @Node(value = "settings.eco.refund.enable")
  public static boolean refundable = true;

  @Node(value = "settings.eco.refund.cost")
  public static double refundCost = 0.00;

  ////////
  @Node(value = "shop.display-items-check-ticks")
  public static int displayItemCheckTicks = 20;

  @Node(value = "shop.ongoing-fee.cost-per-shop")
  public static int ongoingFeeCostPerShop = 20;

  @Node(value = "shop.ongoing-fee.ignore-unlimited")
  public static boolean ongoingFeeIgnoreUnlimited = true;

  @Node(value = "shop.max-shops-checks-in-once")
  public static int maxShopsChecksInOnce = 20;

  @Node(value = "shop.blacklist")
  public static List<String> blacklist = Lists.newArrayList();

  @Node(value = "shop.price-restriction")
  public static List<String> priceRestriction = Lists.newArrayList();

  @Node(value = "shop.sign-material")
  public static String signMaterial = "OAK_WALL_SIGN";

  @Node(value = "shop.disable-vault-format")
  public static boolean disableVaultFormat = false;

  @Node(value = "shop.blacklist-world")
  public static List<String> blacklistWorld = Lists.newArrayList();
  
  @Node(value = "shop.limits.default")
  public static int defaultLimits = 20;
  
  @Node(value = "shop.plugin.OpenInv")
  public static boolean openInv = false;
  
  @Node(value = "shop.plugin.PlaceHolderAPI")
  public static boolean placeHolderAPI = false;
  
  @Node(value = "shop.plugin.enable-metrics")
  public static boolean enableMetrics = true;
  
  @Node(value = "shop.server.uuid", rewrite = true)
  public static String uuid = UUID.randomUUID().toString();
  
  @Node(value = "server.platform")
  public static int serverPlatform = 0;
  
  @Node(value = "shop.update-sign-when-inventory-moving")
  public static boolean updateSignOnInvMove = true;
  
  @Node(value = "shop.sneak-to-control")
  public static boolean sneakToControl = false;
  
  @Node(value = "shop.sneak-to-trade")
  public static boolean sneakToTrade = false;
  
  @Node(value = "shop.sneak-to-creat")
  public static boolean sneakToCreat = false;
  
  @Node(value = "shop.auto-fetch-shop-messages")
  public static boolean autoFetchShopMessages = false;
  
  @Node(value = "shop.display-name-visible")
  public static boolean displayNameVisible = true;
  
  @Node(value = "shop.pay-unlimited-shop-owners")
  public static boolean payUnlimitedShopOwners = false;
  
  @Node(value = "shop.force-load-downgrade-items.enable")
  public static boolean forceLoadDowngradeItems = false;
  
  @Node(value = "shop.force-load-downgrade-items.method")
  public static int forceLoadDowngradeItemsMethod = 0;
  
  @Node(value = "shop.blacklist-lores")
  public static List<String> blacklistLores = Lists.newArrayList();
  
  @Node(value = "shop.use-fast-shop-search-algorithm")
  public static boolean useFastShopSearchAlgorithm = false;
}
