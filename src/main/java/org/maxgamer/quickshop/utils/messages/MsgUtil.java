package org.maxgamer.quickshop.utils.messages;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
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
import org.maxgamer.quickshop.permission.impl.PermissionManager;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopType;
import org.maxgamer.quickshop.shop.api.data.ShopSnapshot;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.files.JsonLocale;
import org.maxgamer.quickshop.utils.files.LocaleFile;
import org.maxgamer.quickshop.utils.nms.ItemNMS;
import com.bekvon.bukkit.residence.commands.info;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

public class MsgUtil {
  private static YamlConfiguration builtInDefaultLanguage = YamlConfiguration.loadConfiguration(
      new InputStreamReader(QuickShop.instance().getLanguage().getFile("en", "messages")));
  
  private static YamlConfiguration enchi18n;
  private static YamlConfiguration itemi18n;
  private static YamlConfiguration potioni18n;
  
  private static LocaleFile messagei18n;
  private static HashMap<UUID, String> playerMessages = Maps.newHashMap();
  
  private static DecimalFormat decimalFormat =
      new DecimalFormat(Objects.requireNonNull(QuickShop.instance().getConfig().getString("decimal-format")));
  
  public final static MinecraftLocale MINECRAFT_LOCALE = new MinecraftLocale();

  /**
   * Translate boolean value to String, the symbon is changeable by language file.
   *
   * @param bool The boolean value
   * @return The result of translate.
   */
  public static String translateBoolean(boolean bool) {
    return MsgUtil.getMessage(bool ? "booleanformat.success" : "booleanformat.failed", null);
  }

  /** Deletes any messages that are older than a week in the database, to save on space. */
  @Deprecated // FIXME Move
  public static void clean() {
    ShopLogger.instance()
        .info("Cleaning purchase messages from the database that are over a week old...");
    // 604800,000 msec = 1 week.
    long weekAgo = System.currentTimeMillis() - 604800000;
    QuickShop.instance().getDatabaseHelper().cleanMessage(weekAgo);
  }

  /**
   * Replace args in raw to args
   *
   * @param raw text
   * @param args args
   * @return filled text
   */
  public static String fillArgs(@Nullable String raw, @Nullable String... args) {
    if (raw == null) {
      return "Invalid message: null";
    }
    if (raw.isEmpty()) {
      return "";
    }
    if (args == null) {
      return raw;
    }
    for (int i = 0; i < args.length; i++) {
      raw = StringUtils.replace(raw, "{" + i + "}", args[i] == null ? "" : args[i]);
    }
    return raw;
  }

  /**
   * Empties the queue of messages a player has and sends them to the player.
   *
   * @param p The player to message
   * @return True if success, False if the player is offline or null
   */
  public static void flushMessagesFor(@NotNull Player player) {
    UUID uuid = player.getUniqueId();
    String message = playerMessages.remove(uuid);
    
    if (message != null) {
      sendMessage(player, message);
      QuickShop.instance().getDatabaseHelper().cleanMessageForPlayer(uuid);
    }
  }

  /**
   * Get Enchantment's i18n name.
   *
   * @param key The Enchantment.
   * @return Enchantment's i18n name.
   */
  public static String getLocalizedName(@NotNull Enchantment enchantment) {
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
  public static String getLocalizedName(@NotNull String itemBukkitName) {
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

  /**
   * getMessage in messages.yml
   *
   * @param loc location
   * @param args args
   * @param player The sender will send the message to
   * @return message
   */
  public static String getMessage(@NotNull String loc,
      @Nullable CommandSender player,
      @NotNull String... args) {
    
    Optional<String> raw = messagei18n.getString(loc);
    if (!raw.isPresent()) {
      Util.debug("ERR: MsgUtil cannot find the the phrase at " + loc
          + ", printing the all readed datas: " + messagei18n);

      return loc;
    }
    String filled = fillArgs(raw.get(), args);
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
   * getMessage in messages.yml
   *
   * @param loc location
   * @param player The sender will send the message to
   * @param args args
   * @return message
   */
  public static String getMessagePlaceholder(
      @NotNull String loc,
      @Nullable OfflinePlayer player,
      @NotNull String... args) {
    
    Optional<String> raw = messagei18n.getString(loc);
    if (!raw.isPresent())
      return loc;
    
    String filled = fillArgs(raw.get(), args);
    
    if (player != null &&
        QuickShop.instance().getPlaceHolderAPI() != null && QuickShop.instance().getPlaceHolderAPI().isEnabled()) {
      filled = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, filled);
      Util.debug("Processed message " + filled + " by PlaceHolderAPI.");
    }
    
    return filled;
  }

  /**
   * Get potion effect's i18n name.
   *
   * @param potion potionType
   * @return Potion's i18n name.
   */
  public static String getPotioni18n(@NotNull PotionEffectType potion) {
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
  
  static Gson gson = new Gson();

  public static void loadCfgMessages() throws InvalidConfigurationException {
    /* Check & Load & Create default messages.yml */
    // Use try block to hook any possible exception, make sure not effect our cfgMessnages code.
    String languageCode = QuickShop.instance().getConfig().getString("language", "en");
    MINECRAFT_LOCALE.reload();
    
    // Init nJson
    LocaleFile json;
    languageCode = "en".equals(languageCode) ?
        languageCode :
          (QuickShop.instance().getResource("messages/" + languageCode + ".json") == null ? "en" : languageCode);
    json = new JsonLocale(new File(QuickShop.instance().getDataFolder(), "messages.json"), "messages/" + languageCode + ".json");
    json.create();
    
    if (!new File(QuickShop.instance().getDataFolder(), "messages.json").exists()) {
      QuickShop.instance().getLanguage().saveFile(languageCode, "messages", "messages.json");
      json.loadFromString(Util.parseColours(Util.readToString(new File(QuickShop.instance().getDataFolder(), "messages.json").getAbsolutePath())));
    } else {
      json.loadFromString(Util.parseColours(json.saveToString()));
    }
    
    messagei18n = json;
    /* Print to console this language file's author, contributors, and region */
    ShopLogger.instance().info(getMessage("translation-author", null));
    ShopLogger.instance().info(getMessage("translation-contributors", null));
    ShopLogger.instance().info(getMessage("translation-country", null));
    /* Save the upgraded messages.yml */
  }

  public static void loadEnchi18n() {
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
        ShopLogger.instance().info("Found new ench [" + enchName + "] , adding it to the config...");
      }
    });
  }
  
  public static void loadItemi18n() {
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
        ShopLogger.instance()
            .info("Found new items/blocks [" + itemName + "] , adding it to the config...");
      }
    });
  }
  
  public static void loadCustomMinecraftLocale(@NotNull String filePrefix, @NotNull Consumer<YamlConfiguration> consumer) {
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

  public static void loadPotioni18n() {
    loadCustomMinecraftLocale("potioni18n", yaml -> {
      potioni18n = yaml;
      
      for (PotionEffectType potion : PotionEffectType.values()) {
        String potionI18n = potioni18n.getString("potioni18n." + potion.getName().trim());
        if (potionI18n != null && !potionI18n.isEmpty()) {
          continue;
        }
        String potionName = MINECRAFT_LOCALE.getPotion(potion);
        ShopLogger.instance().info("Found new potion [" + potionName + "] , adding it to the config...");
        potioni18n.set("potioni18n." + potion.getName(), potionName);
      }
    });
  }

  /** loads all player purchase messages from the database. */
  public static void loadTransactionMessages() {
    playerMessages.clear(); // Delete old messages
    
    try {
      ResultSet rs = QuickShop.instance().getDatabaseHelper().selectAllMessages();
      
      while (rs.next()) {
        String owner = rs.getString("owner");
        UUID ownerUUID;
        if (Util.isUUID(owner)) {
          ownerUUID = UUID.fromString(owner);
        } else {
          ownerUUID = Bukkit.getOfflinePlayer(owner).getUniqueId();
        }
        
        playerMessages.put(ownerUUID, rs.getString("message"));
      }
      
    } catch (Throwable t) {
      ShopLogger.instance().severe("Could not load transaction messages from database.");
      t.printStackTrace();
    }
  }

  /**
   * @param player The name of the player to message
   * @param message The message to send them Sends the given player a message if they're online.
   *        Else, if they're not online, queues it for them in the database.
   * @param isUnlimited The shop is or unlimited
   */
  public static void send(@NotNull UUID uuid, @NotNull String message, boolean isUnlimited) {
    if (isUnlimited && QuickShop.instance().getConfig().getBoolean("shop.ignore-unlimited-shop-messages")) {
      return; // Ignore unlimited shops messages.
    }
    
    Player player = Bukkit.getPlayer(uuid);
    if (player == null) {
      playerMessages.put(uuid, message);
      QuickShop.instance().getDatabaseHelper().sendMessage(uuid, message, System.currentTimeMillis());
    } else {
      sendMessage(player, message);
    }
  }
  
  public static void sendMessage(@NotNull Player player, @NotNull String message) {
    String[] msgData = message.split("##########");
    switch (msgData.length) {
      case 3:
        try {
          sendItemHologram(player, msgData[0], Util.deserialize(msgData[1]), msgData[2]);
        } catch (InvalidConfigurationException e) {
          player.sendMessage(msgData[0].concat(msgData[1]).concat(msgData[2]));
        }
        return;
      case 2:
        try {
          sendItemHologram(player, msgData[0], Util.deserialize(msgData[1]), "");
        } catch (InvalidConfigurationException e) {
          player.sendMessage(msgData[0].concat(msgData[1]));
        }
        return;
      case 1:
      default:
        player.sendMessage(message);
    }
  }

  @SneakyThrows
  public static void sendItemHologram(
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
  public static void sendControlPanelInfo(@NotNull CommandSender sender, @NotNull Shop shop) {
    if (!PermissionManager.instance().has(sender, "quickshop.use")) {
      return;
    }
    if (QuickShop.instance().getConfig().getBoolean("sneak-to-control")) {
      if (sender instanceof Player) {
        if (!((Player) sender).isSneaking()) { // sneaking
          return;
        }
      }
    }
    ChatSheetPrinter chatSheetPrinter = new ChatSheetPrinter(sender);
    chatSheetPrinter.printHeader();
    chatSheetPrinter.printLine(MsgUtil.getMessage("controlpanel.infomation", sender));
    // Owner
    if (!PermissionManager.instance().has(sender, "quickshop.setowner")) {
      chatSheetPrinter.printLine(MsgUtil.getMessage("menu.owner", sender, shop.ownerName()));
    } else {
      chatSheetPrinter
          .printSuggestableCmdLine(
              MsgUtil.getMessage("controlpanel.setowner", sender,
                  shop.ownerName() + ((QuickShop.instance().getConfig().getBoolean(
                      "shop.show-owner-uuid-in-controlpanel-if-op") && shop.isUnlimited())
                          ? (" (" + shop.getOwner() + ")")
                          : "")),
              MsgUtil.getMessage("controlpanel.setowner-hover", sender),
              MsgUtil.getMessage("controlpanel.commands.setowner", sender));
    }

    // Unlimited
    if (PermissionManager.instance().has(sender, "quickshop.unlimited")) {
      String text =
          MsgUtil.getMessage("controlpanel.unlimited", sender, translateBoolean(shop.isUnlimited()));
      String hoverText = MsgUtil.getMessage("controlpanel.unlimited-hover", sender);
      String clickCommand = MsgUtil.getMessage("controlpanel.commands.unlimited", sender,
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
        String text = MsgUtil.getMessage("controlpanel.mode-selling", sender);
        String hoverText = MsgUtil.getMessage("controlpanel.mode-selling-hover", sender);
        String clickCommand = MsgUtil.getMessage("controlpanel.commands.buy", sender,
            shop.getLocation().worldName(),
            String.valueOf(shop.getLocation().x()),
            String.valueOf(shop.getLocation().y()),
            String.valueOf(shop.getLocation().z()));
        chatSheetPrinter.printExecuteableCmdLine(text, hoverText, clickCommand);
      } else {
        String text = MsgUtil.getMessage("controlpanel.mode-buying", sender);
        String hoverText = MsgUtil.getMessage("controlpanel.mode-buying-hover", sender);
        String clickCommand = MsgUtil.getMessage("controlpanel.commands.sell", sender,
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
      String text = MsgUtil.getMessage("controlpanel.price", sender,
          (QuickShop.instance().getConfig().getBoolean("use-decimal-format")) ? decimalFormat(shop.getPrice())
              : "" + shop.getPrice());
      String hoverText = MsgUtil.getMessage("controlpanel.price-hover", sender);
      String clickCommand = MsgUtil.getMessage("controlpanel.commands.price", sender);
      chatSheetPrinter.printSuggestableCmdLine(text, hoverText, clickCommand);
    }
    // Refill
    if (PermissionManager.instance().has(sender, "quickshop.refill")) {
      String text =
          MsgUtil.getMessage("controlpanel.refill", sender, String.valueOf(shop.getPrice()));
      String hoverText = MsgUtil.getMessage("controlpanel.refill-hover", sender);
      String clickCommand = MsgUtil.getMessage("controlpanel.commands.refill", sender);
      chatSheetPrinter.printSuggestableCmdLine(text, hoverText, clickCommand);
    }
    // Refill
    if (PermissionManager.instance().has(sender, "quickshop.empty")) {
      String text =
          MsgUtil.getMessage("controlpanel.empty", sender, String.valueOf(shop.getPrice()));
      String hoverText = MsgUtil.getMessage("controlpanel.empty-hover", sender);
      String clickCommand = MsgUtil.getMessage("controlpanel.commands.empty", sender,
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
          MsgUtil.getMessage("controlpanel.remove", sender, String.valueOf(shop.getPrice()));
      String hoverText = MsgUtil.getMessage("controlpanel.remove-hover", sender);
      String clickCommand = MsgUtil.getMessage("controlpanel.commands.remove", sender,
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
  public static void sendGlobalAlert(@NotNull String content) {
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
  public static void sendItemholochat(@NotNull Shop shop, @NotNull ItemStack itemStack,
      @NotNull Player player, @NotNull String normalText) {
    try {
      String json = ItemNMS.toJson(itemStack);
      if (json == null) {
        return;
      }
      TextComponent normalmessage =
          new TextComponent(normalText + "   " + MsgUtil.getMessage("menu.preview", player));
      ComponentBuilder cBuilder = new ComponentBuilder(json);
      if (PermissionManager.instance().has(player, "quickshop.preview")) {
        normalmessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            MsgUtil.getMessage("menu.commands.preview", player,
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
  public static void sendMessageToOps(@NotNull String message) {
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
  public static void sendPurchaseSuccess(@NotNull Player p, @NotNull Shop shop, int amount, @NotNull ShopSnapshot info) {
    ChatSheetPrinter chatSheetPrinter = new ChatSheetPrinter(p);
    chatSheetPrinter.printHeader();
    chatSheetPrinter.printLine(MsgUtil.getMessage("menu.successful-purchase", p));
    int stacks = info.item().getAmount();
    String stackMessage = stacks > 1 ? " * " + stacks : "";
    chatSheetPrinter.printLine(MsgUtil.getMessage("menu.item-name-and-price", p, "" + amount,
        Util.getItemStackName(shop.getItem()) + stackMessage, Util.format((amount * shop.getPrice()))));
    Map<Enchantment, Integer> enchs = new HashMap<>();
    if (shop.getItem().hasItemMeta()
        && Objects.requireNonNull(shop.getItem().getItemMeta()).hasEnchants()) {
      enchs = shop.getItem().getItemMeta().getEnchants();
    }
    if (!enchs.isEmpty()) {
      chatSheetPrinter.printCenterLine(MsgUtil.getMessage("menu.enchants", p));
      for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
        chatSheetPrinter.printLine(ChatColor.YELLOW + MsgUtil.getLocalizedName(entries.getKey()));
      }
    }
    if (shop.getItem().getItemMeta() instanceof EnchantmentStorageMeta) {
      EnchantmentStorageMeta stor = (EnchantmentStorageMeta) shop.getItem().getItemMeta();
      stor.getStoredEnchants();
      enchs = stor.getStoredEnchants();
      if (!enchs.isEmpty()) {
        chatSheetPrinter.printCenterLine(MsgUtil.getMessage("menu.stored-enchants", p));
        for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
          chatSheetPrinter.printLine(ChatColor.YELLOW + MsgUtil.getLocalizedName(entries.getKey()));
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
  public static void sendSellSuccess(@NotNull Player p, @NotNull Shop shop, int amount) {
    ChatSheetPrinter chatSheetPrinter = new ChatSheetPrinter(p);
    chatSheetPrinter.printHeader();
    chatSheetPrinter.printLine(MsgUtil.getMessage("menu.successfully-sold", p));
    String stackMessage = shop.getItem().getAmount() > 1 ? " * " + shop.getItem().getAmount() : "";
    chatSheetPrinter.printLine(MsgUtil.getMessage("menu.item-name-and-price", p, "" + amount,
        Util.getItemStackName(shop.getItem()) + stackMessage, Util.format((amount * shop.getPrice()))));
    if (QuickShop.instance().getConfig().getBoolean("show-tax")) {
      double tax = QuickShop.instance().getConfig().getDouble("tax");
      double total = amount * shop.getPrice();
      if (tax != 0) {
        if (!p.getUniqueId().equals(shop.getOwner())) {
          chatSheetPrinter
              .printLine(MsgUtil.getMessage("menu.sell-tax", p, Util.format((tax * total))));
        } else {
          chatSheetPrinter.printLine(MsgUtil.getMessage("menu.sell-tax-self", p));
        }
      }
    }
    Map<Enchantment, Integer> enchs = new HashMap<>();
    if (shop.getItem().hasItemMeta()
        && Objects.requireNonNull(shop.getItem().getItemMeta()).hasEnchants()) {
      enchs = shop.getItem().getItemMeta().getEnchants();
    }
    if (!enchs.isEmpty()) {
      chatSheetPrinter.printCenterLine(MsgUtil.getMessage("menu.enchants", p));
      for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
        chatSheetPrinter.printLine(ChatColor.YELLOW + MsgUtil.getLocalizedName(entries.getKey()));
      }
    }
    if (shop.getItem().getItemMeta() instanceof EnchantmentStorageMeta) {
      EnchantmentStorageMeta stor = (EnchantmentStorageMeta) shop.getItem().getItemMeta();
      stor.getStoredEnchants();
      enchs = stor.getStoredEnchants();
      if (!enchs.isEmpty()) {
        chatSheetPrinter.printCenterLine(MsgUtil.getMessage("menu.stored-enchants", p));
        for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
          chatSheetPrinter.printLine(ChatColor.YELLOW + MsgUtil.getLocalizedName(entries.getKey()));
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
  public static void sendShopInfo(@NotNull Player p, @NotNull Shop shop) {
    // Potentially faster with an array?
    ItemStack items = shop.getItem();
    ChatSheetPrinter chatSheetPrinter = new ChatSheetPrinter(p);
    chatSheetPrinter.printHeader();
    chatSheetPrinter.printLine(MsgUtil.getMessage("menu.shop-information", p));
    chatSheetPrinter.printLine(MsgUtil.getMessage("menu.owner", p, shop.ownerName()));
    // Enabled
    String stackMessage = shop.getItem().getAmount() > 1 ? " * " + shop.getItem().getAmount() : "";
    sendItemholochat(shop, items, p,
        ChatColor.DARK_PURPLE + MsgUtil.getMessage("tableformat.left_begin", p) + " "
            + MsgUtil.getMessage("menu.item", p, Util.getItemStackName(items) + stackMessage));
    if (Util.hasDurability(items.getType())) {
      chatSheetPrinter.printLine(
          MsgUtil.getMessage("menu.damage-percent-remaining", p, Util.getToolPercentage(items)));
    }
    if (shop.is(ShopType.SELLING)) {
      if (shop.isUnlimited()) {
        chatSheetPrinter.printLine(
            MsgUtil.getMessage("menu.stock", p, "" + MsgUtil.getMessage("signs.unlimited", p)));
      } else {
        chatSheetPrinter
            .printLine(MsgUtil.getMessage("menu.stock", p, "" + shop.getRemainingStock()));
      }
    } else {
      if (shop.isUnlimited()) {
        chatSheetPrinter.printLine(
            MsgUtil.getMessage("menu.space", p, "" + MsgUtil.getMessage("signs.unlimited", p)));
      } else {
        chatSheetPrinter
            .printLine(MsgUtil.getMessage("menu.space", p, "" + shop.getRemainingSpace()));
      }
    }
    Util.debug("Item type " + shop.getItem().getType());
    chatSheetPrinter.printLine(MsgUtil.getMessage("menu.price-per", p,
        Util.getItemStackName(shop.getItem()), Util.format(shop.getPrice() / shop.getItem().getAmount())));
    if (shop.is(ShopType.BUYING)) {
      chatSheetPrinter.printLine(MsgUtil.getMessage("menu.this-shop-is-buying", p));
    } else {
      chatSheetPrinter.printLine(MsgUtil.getMessage("menu.this-shop-is-selling", p));
    }
    Map<Enchantment, Integer> enchs = new HashMap<>();
    if (items.hasItemMeta() && Objects.requireNonNull(items.getItemMeta()).hasEnchants()) {
      enchs = items.getItemMeta().getEnchants();
    }
    if (!enchs.isEmpty()) {
      chatSheetPrinter.printCenterLine(MsgUtil.getMessage("menu.enchants", p, ""));
      for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
        chatSheetPrinter.printLine(
            ChatColor.YELLOW + MsgUtil.getLocalizedName(entries.getKey()) + " " + entries.getValue());
      }
    }
    if (items.getItemMeta() instanceof EnchantmentStorageMeta) {
      EnchantmentStorageMeta stor = (EnchantmentStorageMeta) items.getItemMeta();
      enchs = stor.getStoredEnchants();
      if (!enchs.isEmpty()) {
        chatSheetPrinter.printLine(MsgUtil.getMessage("menu.stored-enchants", p));
        for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
          chatSheetPrinter.printLine(
              ChatColor.YELLOW + MsgUtil.getLocalizedName(entries.getKey()) + " " + entries.getValue());
        }
      }
    }
    if (items.getItemMeta() instanceof PotionMeta) {
      PotionMeta potionMeta = (PotionMeta) items.getItemMeta();
      PotionEffectType potionEffectType = potionMeta.getBasePotionData().getType().getEffectType();
      if (potionEffectType != null) {
        chatSheetPrinter.printLine(MsgUtil.getMessage("menu.effects", p));
        chatSheetPrinter.printLine(ChatColor.YELLOW + MsgUtil.getPotioni18n(potionEffectType));
      }
      potionMeta.getCustomEffects().forEach((potionEffect -> chatSheetPrinter
          .printLine(ChatColor.YELLOW + MsgUtil.getPotioni18n(potionEffect.getType()))));
    }
    chatSheetPrinter.printFooter();
  }

  public static String decimalFormat(double value) {
    return decimalFormat.format(value);
  }

  public static void setAndUpdate(@NotNull String path, @Nullable Object object) {
    if (object == null) {
      messagei18n.set(path, null); // Removal
    }
    Object objFromBuiltIn = builtInDefaultLanguage.get(path); // Apply english default
    if (objFromBuiltIn == null) {
      objFromBuiltIn = object; // Apply hard-code default, maybe a language file i forgotten
                               // update??
    }
    messagei18n.set(path, objFromBuiltIn);
  }

  public static LocaleFile getI18nFile() {
    return messagei18n;
  }
}
