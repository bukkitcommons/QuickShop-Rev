package org.maxgamer.quickshop.utils.messages;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.nms.ItemNMS;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.LocaleFile;
import cc.bukkit.shop.LocaleManager;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.action.data.ShopSnapshot;
import cc.bukkit.shop.util.ShopLogger;
import cc.bukkit.shop.util.file.json.JsonLocale;
import lombok.SneakyThrows;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class QuickShopLocaleManager implements LocaleManager {
  private YamlConfiguration enchi18n;
  private YamlConfiguration itemi18n;
  private YamlConfiguration potioni18n;
  
  private LocaleFile messagei18n;
  
  public LocaleFile getLocale() {
    return messagei18n;
  }
  
  @NotNull
  public final static MinecraftLocale MINECRAFT_LOCALE = new MinecraftLocale();
  
  private final QuickShop plugin;
  
  public QuickShopLocaleManager(@NotNull QuickShop plugin) {
    this.plugin = plugin;
  }

  /**
   * Translate boolean value to String, the symbon is changeable by language file.
   *
   * @param bool The boolean value
   * @return The result of translate.
   */
  public String translateBoolean(boolean bool) {
    return getMessage(bool ? "booleanformat.success" : "booleanformat.failed");
  }

  /**
   * Get Enchantment's i18n name.
   *
   * @param key The Enchantment.
   * @return Enchantment's i18n name.
   */
  public String getLocalizedName(@NotNull Enchantment enchantment) {
    String namespaceKey = enchantment.getKey().getKey();
    
    String enchI18n = enchi18n.getString("enchi18n.".concat(namespaceKey));
    
    if (enchI18n != null && !enchI18n.isEmpty())
      return enchI18n;
    
    return Util.prettifyText(namespaceKey);
  }

  /**
   * Get item's i18n name, If you want get item name, use Util.getItemStackName
   *
   * @param itemBukkitName ItemBukkitName(e.g. Material.STONE.name())
   * @return String Item's i18n name.
   */
  public String getLocalizedName(@NotNull String itemBukkitName) {
    if (itemBukkitName.isEmpty()) {
      return "Item is empty";
    }
    String itemnameI18n = itemi18n.getString("itemi18n." + itemBukkitName);
    if (itemnameI18n != null && !itemnameI18n.isEmpty()) {
      return itemnameI18n;
    }
    Material material = Material.matchMaterial(itemBukkitName);
    if (material == null) {
      return "Material not exist";
    }
    return Util.prettifyText(material.name());
  }
  
  public String getMessage(@NotNull String loc, @NotNull String... args) {
    return getMessage(loc, null, args);
  }

  /**
   * getMessage in messages.yml
   *
   * @param loc location
   * @param args args
   * @param player The sender will send the message to
   * @return message
   */
  public String getMessage(@NotNull String loc, @Nullable Object player, @NotNull String... args) {
    
    Optional<String> raw = messagei18n.getString(loc);
    if (!raw.isPresent()) {
      Util.debug("ERR: MsgUtil cannot find the the phrase at " + loc
          + ", printing the all readed datas: " + messagei18n);

      return loc;
    }
    String filled = Util.fillArgs(raw.get(), args);
    
    if (player instanceof OfflinePlayer) {
      if (QuickShop.instance().getPlaceHolderAPI() != null && QuickShop.instance().getPlaceHolderAPI().isEnabled()) {
        try {
          filled = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders((OfflinePlayer) player, filled);
          Util.debug("Processed message " + filled + " by PlaceHolderAPI.");
        } catch (Exception ignored) {
          if (((OfflinePlayer) player).getPlayer() != null) {
            try {
              filled = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(((OfflinePlayer) player).getPlayer(), filled);
            } catch (Exception ignore) {
            }
          }
        }
      }
    }
    return filled;
  }

  /**
   * Get potion effect's i18n name.
   *
   * @param potion potionType
   * @return Potion's i18n name.
   */
  public String getLocalizedName(@NotNull PotionEffectType potion) {
    String potionString = potion.getName().trim();
    if (potionString.isEmpty()) {
      return "Potion name is empty.";
    }
    String potionI18n = potioni18n.getString("potioni18n." + potionString);
    if (potionI18n != null && !potionI18n.isEmpty()) {
      return potionI18n;
    }
    return Util.prettifyText(potionString);
  }

  @Override
  public void load() throws InvalidConfigurationException {
    MINECRAFT_LOCALE.reload();
    loadCfgMessages();
    loadEnchi18n();
    loadItemi18n();
    loadPotioni18n();
  }

  public void loadCfgMessages() throws InvalidConfigurationException {
    /* Check & Load & Create default messages.yml */
    // Use try block to hook any possible exception, make sure not effect our cfgMessnages code.
    String languageCode = BaseConfig.language.equalsIgnoreCase("default") ? "en" : BaseConfig.language;
    MINECRAFT_LOCALE.reload();
    
    // Init nJson
    LocaleFile json;
    languageCode = "en".equals(languageCode) ?
        languageCode :
          (QuickShop.instance().getResource("messages/" + languageCode + ".json") == null ? "en" : languageCode);
    json = new JsonLocale(new File(QuickShop.instance().getDataFolder(), "messages.json"), "messages/" + languageCode + ".json", plugin);
    json.create();
    
    if (!new File(QuickShop.instance().getDataFolder(), "messages.json").exists()) {
      plugin.getResourceAccessor().saveFile(languageCode, "messages", "messages.json");
      json.loadFromString(Util.parseColours(Util.readToString(new File(QuickShop.instance().getDataFolder(), "messages.json").getAbsolutePath())));
    } else {
      json.loadFromString(Util.parseColours(json.saveToString()));
    }
    
    messagei18n = json;
    /* Print to console this language file's author, contributors, and region */
    ShopLogger.instance().info(getMessage("translation-author"));
    ShopLogger.instance().info(getMessage("translation-contributors"));
    ShopLogger.instance().info(getMessage("translation-country"));
    /* Save the upgraded messages.yml */
  }

  public void loadEnchi18n() {
    loadCustomMinecraftLocale("enchi18n", yaml -> {
      enchi18n = yaml;
        
      Enchantment[] enchsi18n = Enchantment.values();
      for (Enchantment ench : enchsi18n) {
        String enchi18nString = enchi18n.getString("enchi18n." + ench.getKey().getKey().trim());
        if (enchi18nString != null && !enchi18nString.isEmpty()) {
          continue;
        }
        String enchName = MINECRAFT_LOCALE.getEnchantment(ench.getKey().getKey());
        enchi18n.set("enchi18n." + ench.getKey().getKey(), enchName);
        //ShopLogger.instance().info("Found new ench [" + enchName + "] , adding it to the config...");
      }
    });
  }
  
  public void loadItemi18n() {
    loadCustomMinecraftLocale("itemi18n", yaml -> {
      itemi18n = yaml;
      
      Material[] itemsi18n = Material.values();
      for (Material material : itemsi18n) {
        String itemi18nString = itemi18n.getString("itemi18n." + material.name());
        if (itemi18nString != null && !itemi18nString.isEmpty()) {
          continue;
        }
        String itemName = MINECRAFT_LOCALE.getItem(material);
        itemi18n.set("itemi18n." + material.name(), itemName);
        //ShopLogger.instance()
        //    .info("Found new items/blocks [" + itemName + "] , adding it to the config...");
      }
    });
  }
  
  public void loadCustomMinecraftLocale(@NotNull String filePrefix, @NotNull Consumer<YamlConfiguration> consumer) {
    String fileName = filePrefix.concat(".yml");
    ShopLogger.instance().info("Loading custom locale for " + fileName);
    
    File file = new File(QuickShop.instance().getDataFolder(), fileName);
    QuickShop.instance().saveResource(fileName, true);
    
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    yaml.options().copyDefaults(false);
    YamlConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(QuickShop.instance().getResource(fileName)));
    yaml.setDefaults(def);
    
    Util.parseColours(yaml);
    consumer.accept(yaml);
    
    try {
      yaml.save(file);
      ShopLogger.instance().info("Loaded custom locale: " + fileName);
    } catch (IOException io) {
      ShopLogger.instance().severe("Could not save custom locale for " + fileName);
      io.printStackTrace();
    }
  }

  public void loadPotioni18n() {
    loadCustomMinecraftLocale("potioni18n", yaml -> {
      potioni18n = yaml;
      
      for (PotionEffectType potion : PotionEffectType.values()) {
        String potionI18n = potioni18n.getString("potioni18n." + potion.getName().trim());
        if (potionI18n != null && !potionI18n.isEmpty()) {
          continue;
        }
        String potionName = MINECRAFT_LOCALE.getPotion(potion);
        //ShopLogger.instance().info("Found new potion [" + potionName + "] , adding it to the config...");
        potioni18n.set("potioni18n." + potion.getName(), potionName);
      }
    });
  }

  @SneakyThrows
  public void sendItemHologram(
      @NotNull Player player,
      @NotNull String left,
      @NotNull ItemStack itemStack,
      @NotNull String right) {
    
    String json = ItemNMS.toJson(itemStack);
    if (json == null)
      return;
    
    TextComponent centerItem = new TextComponent(left + Util.getItemStackName(itemStack) + right);
    centerItem.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(json).create()));
    player.spigot().sendMessage(centerItem);
  }

  /**
   * Send controlPanel infomation to sender
   *
   * @param sender Target sender
   * @param shop Target shop
   */
  public void sendControlPanelInfo(@NotNull Player sender, @NotNull ContainerShop shop) {
    if (!PermissionManager.instance().has(sender, "quickshop.use")) {
      return;
    }
    if (BaseConfig.sneakToControl) {
      if (sender instanceof Player) {
        if (!((Player) sender).isSneaking()) { // sneaking
          return;
        }
      }
    }
    ChatSheetPrinter chatSheetPrinter = new ChatSheetPrinter(sender);
    chatSheetPrinter.printHeader();
    chatSheetPrinter.printLine(getMessage("controlpanel.infomation", sender));
    // Owner
    if (!PermissionManager.instance().has(sender, "quickshop.setowner")) {
      chatSheetPrinter.printLine(getMessage("menu.owner", sender, shop.ownerName()));
    } else {
      chatSheetPrinter
          .printSuggestableCmdLine(
              getMessage("controlpanel.setowner", sender,
                  shop.ownerName() + ((BaseConfig.showOwnerUUIDForOp && shop.isUnlimited())
                          ? (" (" + shop.getOwner() + ")")
                          : "")),
              getMessage("controlpanel.setowner-hover", sender),
              getMessage("controlpanel.commands.setowner", sender));
    }

    // Unlimited
    if (PermissionManager.instance().has(sender, "quickshop.unlimited")) {
      String text =
          getMessage("controlpanel.unlimited", sender, translateBoolean(shop.isUnlimited()));
      String hoverText = getMessage("controlpanel.unlimited-hover", sender);
      String clickCommand = getMessage("controlpanel.commands.unlimited", sender,
          shop.getLocation().worldName(),
          String.valueOf(shop.getLocation().x()),
          String.valueOf(shop.getLocation().y()),
          String.valueOf(shop.getLocation().z()));
      chatSheetPrinter.printExecuteableCmdLine(text, hoverText, clickCommand);
    }
    // Buying/Selling Mode
    if (PermissionManager.instance().has(sender, "quickshop.create.buy")
        && sender.hasPermission("quickshop.create.sell")) {
      if (shop.is(ShopType.SELLING)) {
        String text = getMessage("controlpanel.mode-selling", sender);
        String hoverText = getMessage("controlpanel.mode-selling-hover", sender);
        String clickCommand = getMessage("controlpanel.commands.buy", sender,
            shop.getLocation().worldName(),
            String.valueOf(shop.getLocation().x()),
            String.valueOf(shop.getLocation().y()),
            String.valueOf(shop.getLocation().z()));
        chatSheetPrinter.printExecuteableCmdLine(text, hoverText, clickCommand);
      } else {
        String text = getMessage("controlpanel.mode-buying", sender);
        String hoverText = getMessage("controlpanel.mode-buying-hover", sender);
        String clickCommand = getMessage("controlpanel.commands.sell", sender,
            shop.getLocation().worldName(),
            String.valueOf(shop.getLocation().x()),
            String.valueOf(shop.getLocation().y()),
            String.valueOf(shop.getLocation().z()));
        chatSheetPrinter.printExecuteableCmdLine(text, hoverText, clickCommand);
      }
    }
    // Set Price
    if (PermissionManager.instance().has(sender, "quickshop.other.price")
        || shop.getOwner().equals(((OfflinePlayer) sender).getUniqueId())) {
      String text = getMessage("controlpanel.price", sender,
          BaseConfig.decimalFormatPrice ? Util.formatPrice(shop.getPrice()) : "" + shop.getPrice());
      String hoverText = getMessage("controlpanel.price-hover", sender);
      String clickCommand = getMessage("controlpanel.commands.price", sender);
      chatSheetPrinter.printSuggestableCmdLine(text, hoverText, clickCommand);
    }
    // Refill
    if (PermissionManager.instance().has(sender, "quickshop.refill")) {
      String text =
          getMessage("controlpanel.refill", sender, String.valueOf(shop.getPrice()));
      String hoverText = getMessage("controlpanel.refill-hover", sender);
      String clickCommand = getMessage("controlpanel.commands.refill", sender);
      chatSheetPrinter.printSuggestableCmdLine(text, hoverText, clickCommand);
    }
    // Refill
    if (PermissionManager.instance().has(sender, "quickshop.empty")) {
      String text =
          getMessage("controlpanel.empty", sender, String.valueOf(shop.getPrice()));
      String hoverText = getMessage("controlpanel.empty-hover", sender);
      String clickCommand = getMessage("controlpanel.commands.empty", sender,
          shop.getLocation().worldName(),
          String.valueOf(shop.getLocation().x()),
          String.valueOf(shop.getLocation().y()),
          String.valueOf(shop.getLocation().z()));
      chatSheetPrinter.printExecuteableCmdLine(text, hoverText, clickCommand);
    }
    // Remove
    if (PermissionManager.instance().has(sender, "quickshop.other.destroy")
        || shop.getOwner().equals(((OfflinePlayer) sender).getUniqueId())) {
      String text =
          getMessage("controlpanel.remove", sender, String.valueOf(shop.getPrice()));
      String hoverText = getMessage("controlpanel.remove-hover", sender);
      String clickCommand = getMessage("controlpanel.commands.remove", sender,
          shop.getLocation().worldName(),
          String.valueOf(shop.getLocation().x()),
          String.valueOf(shop.getLocation().y()),
          String.valueOf(shop.getLocation().z()));
      chatSheetPrinter.printExecuteableCmdLine(text, hoverText, clickCommand);
    }

    chatSheetPrinter.printFooter();
  }

  /**
   * Send globalAlert to ops, console, log file.
   *
   * @param content The content to send.
   */
  public void sendGlobalAlert(@NotNull String content) {
    sendMessageToOps(content);
    ShopLogger.instance().warning(content);
    QuickShop.instance().getLogWatcher().add(content);
  }

  /**
   * Send the ItemPreview chat msg by NMS.
   *
   * @param shop Target shop
   * @param itemStack Target ItemStack
   * @param player Target player
   * @param normalText The text you will see
   */
  public void sendItemholochat(@NotNull ContainerShop shop, @NotNull ItemStack itemStack,
      @NotNull Player player, @NotNull String normalText) {
    try {
      String json = ItemNMS.toJson(itemStack);
      if (json == null) {
        return;
      }
      TextComponent normalmessage =
          new TextComponent(normalText + "   " + getMessage("menu.preview", player));
      ComponentBuilder cBuilder = new ComponentBuilder(json);
      if (PermissionManager.instance().has(player, "quickshop.preview")) {
        normalmessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            getMessage("menu.commands.preview", player,
                shop.getLocation().worldName(),
                String.valueOf(shop.getLocation().x()),
                String.valueOf(shop.getLocation().y()),
                String.valueOf(shop.getLocation().z()))));
      }
      normalmessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, cBuilder.create()));
      player.spigot().sendMessage(normalmessage);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  /**
   * Send a message for all online Ops.
   *
   * @param message The message you want send
   */
  public void sendMessageToOps(@NotNull String message) {
    Bukkit.getOnlinePlayers()
          .stream()
          .filter(player -> player.isOp() || PermissionManager.instance().has(player, "quickshop.alert"))
          .forEach(player -> player.sendMessage(message));
  }

  /**
   * Send a purchaseSuccess message for a player.
   *
   * @param p Target player
   * @param shop Target shop
   * @param amount Trading item amounts.
   */
  public void sendPurchaseSuccess(@NotNull Player p, @NotNull ContainerShop shop, int amount, @NotNull ShopSnapshot info) {
    ChatSheetPrinter chatSheetPrinter = new ChatSheetPrinter(p);
    chatSheetPrinter.printHeader();
    chatSheetPrinter.printLine(getMessage("menu.successful-purchase", p));
    int stacks = info.item().getAmount();
    String stackMessage = stacks > 1 ? " * " + stacks : "";
    chatSheetPrinter.printLine(getMessage("menu.item-name-and-price", p, "" + amount,
        Util.getItemStackName(shop.getItem()) + stackMessage, Util.format((amount * shop.getPrice()))));
    Map<Enchantment, Integer> enchs = new HashMap<>();
    if (shop.getItem().hasItemMeta()
        && Objects.requireNonNull(shop.getItem().getItemMeta()).hasEnchants()) {
      enchs = shop.getItem().getItemMeta().getEnchants();
    }
    if (!enchs.isEmpty()) {
      chatSheetPrinter.printCenterLine(getMessage("menu.enchants", p));
      for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
        chatSheetPrinter.printLine(ChatColor.YELLOW + getLocalizedName(entries.getKey()));
      }
    }
    if (shop.getItem().getItemMeta() instanceof EnchantmentStorageMeta) {
      EnchantmentStorageMeta stor = (EnchantmentStorageMeta) shop.getItem().getItemMeta();
      stor.getStoredEnchants();
      enchs = stor.getStoredEnchants();
      if (!enchs.isEmpty()) {
        chatSheetPrinter.printCenterLine(getMessage("menu.stored-enchants", p));
        for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
          chatSheetPrinter.printLine(ChatColor.YELLOW + getLocalizedName(entries.getKey()));
        }
      }
    }
    chatSheetPrinter.printFooter();
  }

  /**
   * Send a sellSuccess message for a player.
   *
   * @param p Target player
   * @param shop Target shop
   * @param amount Trading item amounts.
   */
  public void sendSellSuccess(@NotNull Player p, @NotNull ContainerShop shop, int amount) {
    ChatSheetPrinter chatSheetPrinter = new ChatSheetPrinter(p);
    chatSheetPrinter.printHeader();
    chatSheetPrinter.printLine(getMessage("menu.successfully-sold", p));
    String stackMessage = shop.getItem().getAmount() > 1 ? " * " + shop.getItem().getAmount() : "";
    chatSheetPrinter.printLine(getMessage("menu.item-name-and-price", p, "" + amount,
        Util.getItemStackName(shop.getItem()) + stackMessage, Util.format((amount * shop.getPrice()))));
    if (BaseConfig.showTax) {
      double tax = BaseConfig.taxRate;
      double total = amount * shop.getPrice();
      if (tax != 0) {
        if (!p.getUniqueId().equals(shop.getOwner())) {
          chatSheetPrinter
              .printLine(getMessage("menu.sell-tax", p, Util.format((tax * total))));
        } else {
          chatSheetPrinter.printLine(getMessage("menu.sell-tax-self", p));
        }
      }
    }
    Map<Enchantment, Integer> enchs = new HashMap<>();
    if (shop.getItem().hasItemMeta()
        && Objects.requireNonNull(shop.getItem().getItemMeta()).hasEnchants()) {
      enchs = shop.getItem().getItemMeta().getEnchants();
    }
    if (!enchs.isEmpty()) {
      chatSheetPrinter.printCenterLine(getMessage("menu.enchants", p));
      for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
        chatSheetPrinter.printLine(ChatColor.YELLOW + getLocalizedName(entries.getKey()));
      }
    }
    if (shop.getItem().getItemMeta() instanceof EnchantmentStorageMeta) {
      EnchantmentStorageMeta stor = (EnchantmentStorageMeta) shop.getItem().getItemMeta();
      stor.getStoredEnchants();
      enchs = stor.getStoredEnchants();
      if (!enchs.isEmpty()) {
        chatSheetPrinter.printCenterLine(getMessage("menu.stored-enchants", p));
        for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
          chatSheetPrinter.printLine(ChatColor.YELLOW + getLocalizedName(entries.getKey()));
        }
      }
    }
    chatSheetPrinter.printFooter();
  }

  /**
   * Send a shop infomation to a player.
   *
   * @param p Target player
   * @param shop The shop
   */
  public void sendShopInfo(@NotNull Player p, @NotNull ContainerShop shop) {
    // Potentially faster with an array?
    ItemStack items = shop.getItem();
    ChatSheetPrinter chatSheetPrinter = new ChatSheetPrinter(p);
    chatSheetPrinter.printHeader();
    chatSheetPrinter.printLine(getMessage("menu.shop-information", p));
    chatSheetPrinter.printLine(getMessage("menu.owner", p, shop.ownerName()));
    // Enabled
    String stackMessage = shop.getItem().getAmount() > 1 ? " * " + shop.getItem().getAmount() : "";
    sendItemholochat(shop, items, p,
        ChatColor.DARK_PURPLE + getMessage("tableformat.left_begin", p) + " "
            + getMessage("menu.item", p, Util.getItemStackName(items) + stackMessage));
    if (Util.hasDurability(items.getType())) {
      chatSheetPrinter.printLine(
          getMessage("menu.damage-percent-remaining", p, Util.getToolPercentage(items)));
    }
    if (shop.is(ShopType.SELLING)) {
      if (shop.isUnlimited()) {
        chatSheetPrinter.printLine(
            getMessage("menu.stock", p, "" + getMessage("signs.unlimited", p)));
      } else {
        chatSheetPrinter
            .printLine(getMessage("menu.stock", p, "" + shop.getRemainingStock()));
      }
    } else {
      if (shop.isUnlimited()) {
        chatSheetPrinter.printLine(
            getMessage("menu.space", p, "" + getMessage("signs.unlimited", p)));
      } else {
        chatSheetPrinter
            .printLine(getMessage("menu.space", p, "" + shop.getRemainingSpace()));
      }
    }
    Util.debug("Item type " + shop.getItem().getType());
    chatSheetPrinter.printLine(getMessage("menu.price-per", p,
        Util.getItemStackName(shop.getItem()), Util.format(shop.getPrice() / shop.getItem().getAmount())));
    if (shop.is(ShopType.BUYING)) {
      chatSheetPrinter.printLine(getMessage("menu.this-shop-is-buying", p));
    } else {
      chatSheetPrinter.printLine(getMessage("menu.this-shop-is-selling", p));
    }
    Map<Enchantment, Integer> enchs = new HashMap<>();
    if (items.hasItemMeta() && Objects.requireNonNull(items.getItemMeta()).hasEnchants()) {
      enchs = items.getItemMeta().getEnchants();
    }
    if (!enchs.isEmpty()) {
      chatSheetPrinter.printCenterLine(getMessage("menu.enchants", p, ""));
      for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
        chatSheetPrinter.printLine(
            ChatColor.YELLOW + getLocalizedName(entries.getKey()) + " " + entries.getValue());
      }
    }
    if (items.getItemMeta() instanceof EnchantmentStorageMeta) {
      EnchantmentStorageMeta stor = (EnchantmentStorageMeta) items.getItemMeta();
      enchs = stor.getStoredEnchants();
      if (!enchs.isEmpty()) {
        chatSheetPrinter.printLine(getMessage("menu.stored-enchants", p));
        for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
          chatSheetPrinter.printLine(
              ChatColor.YELLOW + getLocalizedName(entries.getKey()) + " " + entries.getValue());
        }
      }
    }
    if (items.getItemMeta() instanceof PotionMeta) {
      PotionMeta potionMeta = (PotionMeta) items.getItemMeta();
      PotionEffectType potionEffectType = potionMeta.getBasePotionData().getType().getEffectType();
      if (potionEffectType != null) {
        chatSheetPrinter.printLine(getMessage("menu.effects", p));
        chatSheetPrinter.printLine(ChatColor.YELLOW + getLocalizedName(potionEffectType));
      }
      potionMeta.getCustomEffects().forEach((potionEffect -> chatSheetPrinter
          .printLine(ChatColor.YELLOW + getLocalizedName(potionEffect.getType()))));
    }
    chatSheetPrinter.printFooter();
  }

  public LocaleFile getI18nFile() {
    return messagei18n;
  }
}
