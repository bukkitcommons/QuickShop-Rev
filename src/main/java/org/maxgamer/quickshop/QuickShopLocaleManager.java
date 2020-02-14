package org.maxgamer.quickshop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.messages.ChatSheetPrinter;
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.utils.ItemNMS;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.JavaUtils;
import org.maxgamer.quickshop.utils.ShopUtils;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.LocaleManager;
import cc.bukkit.shop.MinecraftLocaleProvider;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.action.data.ShopSnapshot;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.util.file.JsonReader;
import cc.bukkit.shop.util.file.ResourceAccessor;
import cc.bukkit.shop.util.locale.MinecraftLocale;
import lombok.SneakyThrows;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class QuickShopLocaleManager implements LocaleManager {
  private FileConfiguration pluginLocale;
  
  @NotNull
  public MinecraftLocale minecraftLocale;
  
  public QuickShopLocaleManager() {
    this.minecraftLocale = new MinecraftLocale(new File(Shop.instance().getDataFolder(), "cache"));
  }
  
  /**
   * Get Enchantment's i18n name.
   *
   * @param key The Enchantment.
   * @return Enchantment's i18n name.
   */
  public String get(@NotNull Enchantment enchantment) {
    return minecraftLocale.get(enchantment);
  }
  
  public String get(@NotNull String loc, @NotNull String... args) {
    return get(loc, null, args);
  }

  /**
   * get in messages.yml
   *
   * @param loc location
   * @param args args
   * @param player The sender will send the message to
   * @return message
   */
  public String get(@NotNull String loc, @Nullable Object player, @NotNull String... args) {
    
    String raw = pluginLocale.getString(loc);
    if (raw == null) {
      Util.debug("ERR: MsgUtil cannot find the the phrase at " + loc
          + ", printing the all readed datas: " + pluginLocale);

      return loc;
    }
    String filled = JavaUtils.fillArgs(raw, args);
    
    if (player instanceof OfflinePlayer) {
      if (QuickShop.instance().getPlaceHolderAPI().isPresent() && QuickShop.instance().getPlaceHolderAPI().get().isEnabled()) {
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
  
  public void sendParsed(@NotNull UUID player, @NotNull String message, boolean isUnlimited) {
    if (BaseConfig.ignoreUnlimitedMessages && isUnlimited) {
      return; // Ignore unlimited shops messages.
    }
    String[] msgData = message.split("##########");
    Player p = Bukkit.getPlayer(player);
    if (p != null) {
      try {
        sendItemHologram(p, msgData[0], ItemUtils.deserialize(msgData[1]), msgData[2]);
      } catch (InvalidConfigurationException e) {
        p.sendMessage(msgData[0] + msgData[1] + msgData[2]);
      } catch (ArrayIndexOutOfBoundsException e2) {
        try {
          sendItemHologram(p, msgData[0], ItemUtils.deserialize(msgData[1]), "");
        } catch (Exception any) {
          p.sendMessage(message);
        }
      }
    }
  }

  /**
   * Get potion effect's i18n name.
   *
   * @param potion potionType
   * @return Potion's i18n name.
   */
  public String get(@NotNull PotionEffectType potion) {
    return minecraftLocale.get(potion);
  }

  @Override
  @SneakyThrows
  public void load() throws InvalidConfigurationException {
    minecraftLocale.load(BaseConfig.language);
    loadCfgMessages();
  }
  
  private final static String DEFAULT_LOCALE = "en_US";

  public void loadCfgMessages() throws InvalidConfigurationException, IOException {
    /* Check & Load & Create default messages.yml */
    // Use try block to hook any possible exception, make sure not effect our cfgMessnages code.
    String languageCode = BaseConfig.language.equalsIgnoreCase("default") ? DEFAULT_LOCALE : BaseConfig.language;
    languageCode = languageCode.replace('-', '_');
    
    // Init nJson
    FileConfiguration json;
    languageCode = DEFAULT_LOCALE.equalsIgnoreCase(languageCode) ?
        languageCode :
          (Shop.instance().getResource("messages/" + languageCode + ".json") == null ? DEFAULT_LOCALE : languageCode);
    json = JsonReader.read(new File(QuickShop.instance().getDataFolder(), "messages.json"));
    
    if (!new File(QuickShop.instance().getDataFolder(), "messages.json").exists()) {
      ResourceAccessor.save("messages/" + languageCode + ".json", "messages.json");
      json.loadFromString(Util.parseColours(JavaUtils.readToString(new File(QuickShop.instance().getDataFolder(), "messages.json").getAbsolutePath())));
    } else {
      json.loadFromString(Util.parseColours(json.saveToString()));
    }
    
    pluginLocale = json;
    /* Print to console this language file's author, contributors, and region */
    ShopLogger.instance().info(get("translation-author"));
    ShopLogger.instance().info(get("translation-contributors"));
    ShopLogger.instance().info(get("translation-country"));
    /* Save the upgraded messages.yml */
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
    
    TextComponent centerItem = new TextComponent(left + ItemUtils.getItemStackName(itemStack) + right);
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
    if (!QuickShopPermissionManager.instance().has(sender, "quickshop.use")) {
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
    chatSheetPrinter.printLine(get("controlpanel.infomation", sender));
    // Owner
    if (!QuickShopPermissionManager.instance().has(sender, "quickshop.setowner")) {
      chatSheetPrinter.printLine(get("menu.owner", sender, shop.ownerName()));
    } else {
      chatSheetPrinter
          .printSuggestableCmdLine(
              get("controlpanel.setowner", sender,
                  shop.ownerName() + ((BaseConfig.showOwnerUUIDForOp && shop.isUnlimited())
                          ? (" (" + shop.getOwner() + ")")
                          : "")),
              get("controlpanel.setowner-hover", sender),
              get("controlpanel.commands.setowner", sender));
    }

    // Unlimited
    if (QuickShopPermissionManager.instance().has(sender, "quickshop.unlimited")) {
      String text =
          get("controlpanel.unlimited", sender, get(shop.isUnlimited()));
      String hoverText = get("controlpanel.unlimited-hover", sender);
      String clickCommand = get("controlpanel.commands.unlimited", sender,
          shop.getLocation().worldName(),
          String.valueOf(shop.getLocation().x()),
          String.valueOf(shop.getLocation().y()),
          String.valueOf(shop.getLocation().z()));
      chatSheetPrinter.printExecuteableCmdLine(text, hoverText, clickCommand);
    }
    // Buying/Selling Mode
    if (QuickShopPermissionManager.instance().has(sender, "quickshop.create.buy")
        && sender.hasPermission("quickshop.create.sell")) {
      if (shop.is(ShopType.SELLING)) {
        String text = get("controlpanel.mode-selling", sender);
        String hoverText = get("controlpanel.mode-selling-hover", sender);
        String clickCommand = get("controlpanel.commands.buy", sender,
            shop.getLocation().worldName(),
            String.valueOf(shop.getLocation().x()),
            String.valueOf(shop.getLocation().y()),
            String.valueOf(shop.getLocation().z()));
        chatSheetPrinter.printExecuteableCmdLine(text, hoverText, clickCommand);
      } else {
        String text = get("controlpanel.mode-buying", sender);
        String hoverText = get("controlpanel.mode-buying-hover", sender);
        String clickCommand = get("controlpanel.commands.sell", sender,
            shop.getLocation().worldName(),
            String.valueOf(shop.getLocation().x()),
            String.valueOf(shop.getLocation().y()),
            String.valueOf(shop.getLocation().z()));
        chatSheetPrinter.printExecuteableCmdLine(text, hoverText, clickCommand);
      }
    }
    // Set Price
    if (QuickShopPermissionManager.instance().has(sender, "quickshop.other.price")
        || shop.getOwner().equals(((OfflinePlayer) sender).getUniqueId())) {
      String text = get("controlpanel.price", sender,
          BaseConfig.decimalFormatPrice ? ShopUtils.formatPrice(shop.getPrice()) : "" + shop.getPrice());
      String hoverText = get("controlpanel.price-hover", sender);
      String clickCommand = get("controlpanel.commands.price", sender);
      chatSheetPrinter.printSuggestableCmdLine(text, hoverText, clickCommand);
    }
    // Refill
    if (QuickShopPermissionManager.instance().has(sender, "quickshop.refill")) {
      String text =
          get("controlpanel.refill", sender, String.valueOf(shop.getPrice()));
      String hoverText = get("controlpanel.refill-hover", sender);
      String clickCommand = get("controlpanel.commands.refill", sender);
      chatSheetPrinter.printSuggestableCmdLine(text, hoverText, clickCommand);
    }
    // Refill
    if (QuickShopPermissionManager.instance().has(sender, "quickshop.empty")) {
      String text =
          get("controlpanel.empty", sender, String.valueOf(shop.getPrice()));
      String hoverText = get("controlpanel.empty-hover", sender);
      String clickCommand = get("controlpanel.commands.empty", sender,
          shop.getLocation().worldName(),
          String.valueOf(shop.getLocation().x()),
          String.valueOf(shop.getLocation().y()),
          String.valueOf(shop.getLocation().z()));
      chatSheetPrinter.printExecuteableCmdLine(text, hoverText, clickCommand);
    }
    // Remove
    if (QuickShopPermissionManager.instance().has(sender, "quickshop.other.destroy")
        || shop.getOwner().equals(((OfflinePlayer) sender).getUniqueId())) {
      String text =
          get("controlpanel.remove", sender, String.valueOf(shop.getPrice()));
      String hoverText = get("controlpanel.remove-hover", sender);
      String clickCommand = get("controlpanel.commands.remove", sender,
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
          new TextComponent(normalText + "   " + get("menu.preview", player));
      ComponentBuilder cBuilder = new ComponentBuilder(json);
      if (QuickShopPermissionManager.instance().has(player, "quickshop.preview")) {
        normalmessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            get("menu.commands.preview", player,
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
          .filter(player -> player.isOp() || QuickShopPermissionManager.instance().has(player, "quickshop.alert"))
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
    chatSheetPrinter.printLine(get("menu.successful-purchase", p));
    int stacks = info.item().getAmount();
    String stackMessage = stacks > 1 ? " * " + stacks : "";
    chatSheetPrinter.printLine(get("menu.item-name-and-price", p, "" + amount,
        ItemUtils.getItemStackName(shop.getItem()) + stackMessage, JavaUtils.format((amount * shop.getPrice()))));
    Map<Enchantment, Integer> enchs = new HashMap<>();
    if (shop.getItem().hasItemMeta()
        && Objects.requireNonNull(shop.getItem().getItemMeta()).hasEnchants()) {
      enchs = shop.getItem().getItemMeta().getEnchants();
    }
    if (!enchs.isEmpty()) {
      chatSheetPrinter.printCenterLine(get("menu.enchants", p));
      for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
        chatSheetPrinter.printLine(ChatColor.YELLOW + get(entries.getKey()));
      }
    }
    if (shop.getItem().getItemMeta() instanceof EnchantmentStorageMeta) {
      EnchantmentStorageMeta stor = (EnchantmentStorageMeta) shop.getItem().getItemMeta();
      stor.getStoredEnchants();
      enchs = stor.getStoredEnchants();
      if (!enchs.isEmpty()) {
        chatSheetPrinter.printCenterLine(get("menu.stored-enchants", p));
        for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
          chatSheetPrinter.printLine(ChatColor.YELLOW + get(entries.getKey()));
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
    chatSheetPrinter.printLine(get("menu.successfully-sold", p));
    String stackMessage = shop.getItem().getAmount() > 1 ? " * " + shop.getItem().getAmount() : "";
    chatSheetPrinter.printLine(get("menu.item-name-and-price", p, "" + amount,
        ItemUtils.getItemStackName(shop.getItem()) + stackMessage, JavaUtils.format((amount * shop.getPrice()))));
    if (BaseConfig.showTax) {
      double tax = BaseConfig.taxRate;
      double total = amount * shop.getPrice();
      if (tax != 0) {
        if (!p.getUniqueId().equals(shop.getOwner())) {
          chatSheetPrinter
              .printLine(get("menu.sell-tax", p, JavaUtils.format((tax * total))));
        } else {
          chatSheetPrinter.printLine(get("menu.sell-tax-self", p));
        }
      }
    }
    Map<Enchantment, Integer> enchs = new HashMap<>();
    if (shop.getItem().hasItemMeta()
        && Objects.requireNonNull(shop.getItem().getItemMeta()).hasEnchants()) {
      enchs = shop.getItem().getItemMeta().getEnchants();
    }
    if (!enchs.isEmpty()) {
      chatSheetPrinter.printCenterLine(get("menu.enchants", p));
      for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
        chatSheetPrinter.printLine(ChatColor.YELLOW + get(entries.getKey()));
      }
    }
    if (shop.getItem().getItemMeta() instanceof EnchantmentStorageMeta) {
      EnchantmentStorageMeta stor = (EnchantmentStorageMeta) shop.getItem().getItemMeta();
      stor.getStoredEnchants();
      enchs = stor.getStoredEnchants();
      if (!enchs.isEmpty()) {
        chatSheetPrinter.printCenterLine(get("menu.stored-enchants", p));
        for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
          chatSheetPrinter.printLine(ChatColor.YELLOW + get(entries.getKey()));
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
    chatSheetPrinter.printLine(get("menu.shop-information", p));
    chatSheetPrinter.printLine(get("menu.owner", p, shop.ownerName()));
    // Enabled
    String stackMessage = shop.getItem().getAmount() > 1 ? " * " + shop.getItem().getAmount() : "";
    sendItemholochat(shop, items, p,
        ChatColor.DARK_PURPLE + get("tableformat.left_begin", p) + " "
            + get("menu.item", p, ItemUtils.getItemStackName(items) + stackMessage));
    if (ItemUtils.hasDurability(items.getType())) {
      chatSheetPrinter.printLine(
          get("menu.damage-percent-remaining", p, ItemUtils.getToolPercentage(items)));
    }
    if (shop.is(ShopType.SELLING)) {
      if (shop.isUnlimited()) {
        chatSheetPrinter.printLine(
            get("menu.stock", p, "" + get("signs.unlimited", p)));
      } else {
        chatSheetPrinter
            .printLine(get("menu.stock", p, "" + shop.getRemainingStock()));
      }
    } else {
      if (shop.isUnlimited()) {
        chatSheetPrinter.printLine(
            get("menu.space", p, "" + get("signs.unlimited", p)));
      } else {
        chatSheetPrinter
            .printLine(get("menu.space", p, "" + shop.getRemainingSpace()));
      }
    }
    Util.debug("Item type " + shop.getItem().getType());
    chatSheetPrinter.printLine(get("menu.price-per", p,
        ItemUtils.getItemStackName(shop.getItem()), JavaUtils.format(shop.getPrice() / shop.getItem().getAmount())));
    if (shop.is(ShopType.BUYING)) {
      chatSheetPrinter.printLine(get("menu.this-shop-is-buying", p));
    } else {
      chatSheetPrinter.printLine(get("menu.this-shop-is-selling", p));
    }
    Map<Enchantment, Integer> enchs = new HashMap<>();
    if (items.hasItemMeta() && Objects.requireNonNull(items.getItemMeta()).hasEnchants()) {
      enchs = items.getItemMeta().getEnchants();
    }
    if (!enchs.isEmpty()) {
      chatSheetPrinter.printCenterLine(get("menu.enchants", p, ""));
      for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
        chatSheetPrinter.printLine(
            ChatColor.YELLOW + get(entries.getKey()) + " " + entries.getValue());
      }
    }
    if (items.getItemMeta() instanceof EnchantmentStorageMeta) {
      EnchantmentStorageMeta stor = (EnchantmentStorageMeta) items.getItemMeta();
      enchs = stor.getStoredEnchants();
      if (!enchs.isEmpty()) {
        chatSheetPrinter.printLine(get("menu.stored-enchants", p));
        for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
          chatSheetPrinter.printLine(
              ChatColor.YELLOW + get(entries.getKey()) + " " + entries.getValue());
        }
      }
    }
    if (items.getItemMeta() instanceof PotionMeta) {
      PotionMeta potionMeta = (PotionMeta) items.getItemMeta();
      PotionEffectType potionEffectType = potionMeta.getBasePotionData().getType().getEffectType();
      if (potionEffectType != null) {
        chatSheetPrinter.printLine(get("menu.effects", p));
        chatSheetPrinter.printLine(ChatColor.YELLOW + get(potionEffectType));
      }
      potionMeta.getCustomEffects().forEach((potionEffect -> chatSheetPrinter
          .printLine(ChatColor.YELLOW + get(potionEffect.getType()))));
    }
    chatSheetPrinter.printFooter();
  }

  @Override
  public void load(@NotNull String locale) throws IOException {
    minecraftLocale.load(locale);
  }

  @Override
  public Optional<String> get(@NotNull String key) {
    return minecraftLocale.get(key);
  }

  @Override
  public MinecraftLocaleProvider getMinecraftLocale() {
    return minecraftLocale;
  }
}
