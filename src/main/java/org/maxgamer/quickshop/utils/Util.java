package org.maxgamer.quickshop.utils;

import com.google.common.io.Files;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.EnderChest;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.database.connector.MySQLConnector;
import org.maxgamer.quickshop.shop.ShopLoader;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.data.ShopLocation;
import org.maxgamer.quickshop.shop.hologram.DisplayItem;
import org.maxgamer.quickshop.utils.messages.Colorizer;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.messages.ShopPluginLogger;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/** @author MACHENIKE */
/**
 * Utility class for plugin generally helper methods
 * 
 * @author Ghost_chu
 * @author Sotr
 */
public class Util {
  static short tookLongTimeCostTimes;
  private static EnumSet<Material> blacklist = EnumSet.noneOf(Material.class);
  @Getter
  private static List<String> debugLogs = new LinkedList<>();
  private static EnumMap<Material, Entry<Double, Double>> restrictedPrices =
      new EnumMap<>(Material.class);
  private static Object serverInstance;
  private static EnumSet<Material> blockListedBlocks = EnumSet.noneOf(Material.class);
  private static Field tpsField;
  private static List<String> worldBlacklist = new ArrayList<>();
  
  public static Location getCenter(@NotNull ShopLocation shopLocation) {
    // This is always '+' instead of '-' even in negative pos
    return new Location(shopLocation.world(), shopLocation.x() + .5, shopLocation.y() + .5,
        shopLocation.z() + .5);
  }

  /**
   * Gets an unique key of a chunk based on its coordinates.
   * 
   * @param x X Coordinate
   * @param z Z Coordinate
   * @return Chunk coordinates packed into a long
   * @author Aikar
   */
  public static long chunkKey(int chunkX, int chunkZ) {
    return (long) chunkX & 0xffffffffL | ((long) chunkZ & 0xffffffffL) << 32;
  }

  /**
   * Gets an unique key of a block based on its coordinates.
   * 
   * @param x X Coordinate
   * @param z Z Coordinate
   * @return Chunk coordinates packed into a long
   * @author Spottedleaf
   */
  public static long blockKey(int x, int y, int z) {
    return ((long) x & 0x7FFFFFF) | (((long) z & 0x7FFFFFF) << 27) | ((long) y << 54);
  }
  
  public static long blockKey(Location loc) {
    return blockKey(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  /**
   * Gets the shop a sign is attached to
   *
   * @param b The location of the sign
   * @return The shop
   */
  public static ShopViewer getShopBySign(@NotNull Block sign) {
    final Optional<Block> shopBlock = Util.getSignAttached(sign);
    
    return shopBlock.isPresent() ?
        ShopManager
          .instance()
          .getLoadedShopAt(shopBlock.get()) : ShopViewer.empty();
  }

  /**
   * Convert strArray to String. E.g "Foo, Bar"
   *
   * @param strArray Target array
   * @return str
   */
  public static String array2String(@NotNull String[] strArray) {
    switch (strArray.length) {
      case 0:
        return "";
      case 1:
        return strArray[0];
      case 2:
        return strArray[0].concat(", ").concat(strArray[1]);
      default:
        String con = strArray[0].concat(", ");
        for (int i = 0; i < strArray.length - 2; i++) {
          con = con.concat(strArray[i]).concat(", ");
        }
        return con.concat(strArray[strArray.length - 1]);
    }
  }

  /**
   * Backup shops.db
   *
   * @return The result for backup
   */
  public static boolean backupDatabase() {
    if (QuickShop.instance().getDatabase().getConnector() instanceof MySQLConnector) {
      return true; // Backup and logs by MySQL
    }
    File dataFolder = QuickShop.instance().getDataFolder();
    File sqlfile = new File(dataFolder, "shops.db");
    if (!sqlfile.exists()) {
      ShopLogger.instance().warning("Failed to backup! (File not found)");
      return false;
    }
    String uuid = UUID.randomUUID().toString().replaceAll("_", "");
    File bksqlfile = new File(dataFolder, "/shops_backup_" + uuid + ".db");
    try {
      Files.copy(sqlfile, bksqlfile);
    } catch (Exception e1) {
      e1.printStackTrace();
      ShopLogger.instance().warning("Failed to backup the database.");
      return false;
    }
    return true;
  }
  
  public static boolean canBeShop(@NotNull Block block) {
    // Specificed types by configuration
    if (isBlocklisted(block.getType()) || isBlacklistWorld(block.getWorld())) {
      return false;
    }
    return canBeShopIgnoreBlocklist(block.getState());
  }
  
  public static boolean canBeShop(@NotNull BlockState state) {
    if (isBlocklisted(state.getType()) || isBlacklistWorld(state.getWorld())) {
      return false;
    }
    return canBeShopIgnoreBlocklist(state);
  }

  /**
   * Returns true if the given block could be used to make a shop out of.
   *
   * @param b The block to check, Possibly a chest, dispenser, etc.
   * @return True if it can be made into a shop, otherwise false.
   */
  public static boolean canBeShopIgnoreBlocklist(@NotNull BlockState state) {
    if (state instanceof EnderChest) { // BlockState for Mod supporting
      return QuickShop.instance().getOpenInvPlugin() != null;
    }

    return state instanceof InventoryHolder;
  }

  /**
   * Counts the number of items in the given inventory where Util.matches(inventory item, item) is
   * true.
   *
   * @param inv The inventory to search
   * @param item The ItemStack to search for
   * @return The number of items that match in this inventory.
   */
  public static int countStacks(@Nullable Inventory inv, @NotNull ItemStack item) {
    if (inv == null)
      return 0;
    
    int items = 0;
    for (ItemStack iStack : inv.getStorageContents()) {
      if (iStack == null || iStack.getType() == Material.AIR)
        continue;
      
      if (QuickShop.instance().getItemMatcher().matches(item, iStack, false)) {
        items += iStack.getAmount();
      }
    }
    
    return items / item.getAmount();
  }

  /**
   * Returns the number of items that can be given to the inventory safely.
   *
   * @param inv The inventory to count
   * @param item The item prototype. Material, durabiltiy and enchants must match for 'stackability'
   *        to occur.
   * @return The number of items that can be given to the inventory safely.
   */
  public static int countSpace(@Nullable Inventory inv, @NotNull ItemStack item) {
    if (inv == null)
      return 0;
    
    int space = 0;

    ItemStack[] contents = inv.getStorageContents();
    for (ItemStack iStack : contents) {
      if (iStack == null || iStack.getType() == Material.AIR) {
        space += item.getMaxStackSize();
      } else if (QuickShop.instance().getItemMatcher().matches(item, iStack, false)) {
        space += (item.getMaxStackSize() - iStack.getAmount());
      }
    }
    
    return space;
  }

  /**
   * Print debug log when plugin running on dev mode.
   *
   * @param logs logs
   */
  public static void debug(@NotNull String... logs) {
    long startTime = System.currentTimeMillis();
    StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
    String className = stackTraceElement.getClassName();
    try {
      Class<?> c = Class.forName(className);
      className = c.getSimpleName();
      if (!c.getSimpleName().isEmpty()) {
        className = c.getSimpleName();
      }
    } catch (ClassNotFoundException e) {
      // Ignore
    }
    //String methodName = stackTraceElement.getMethodName();
    int codeLine = stackTraceElement.getLineNumber();

    for (String log : logs) {
      String text = "["
          // + ChatColor.DARK_GREEN
          // + ChatColor.BOLD
          // + "DEBUG"
          // + ChatColor.RESET
          // + "] ["
          + ChatColor.DARK_GREEN + className + ChatColor.RESET + "] ("
          /*
           * + " [" + ChatColor.DARK_GREEN + methodName + ChatColor.RESET
           */
          + ChatColor.DARK_GREEN + codeLine + ChatColor.RESET + ") " + log;
      debugLogs.add(Colorizer.stripColors(text));
      if (debugLogs.size() > 500000) /* Keep debugLogs max can have 500k lines. */ {
        debugLogs.remove(0);
      }
      ShopLogger.instance().info(text);
    }
    long debugLogCost = System.currentTimeMillis() - startTime;
    if (false) {
      if (debugLogCost > 5) {
        tookLongTimeCostTimes++;
        if (tookLongTimeCostTimes > 15000) {
          BaseConfig.debugLogger = false;
          QuickShop.instance().getConfigurationManager().save(BaseConfig.class);
          QuickShop.instance().saveConfig();
          ShopLogger.instance().warning(
              "Detected the debug logger tooked time keep too lang, QuickShop already auto-disable debug logger, your server performance should back to normal. But you must re-enable it if you want to report any bugs.");
        }
      }
    }
  }

  /**
   * Covert YAML string to ItemStack.
   *
   * @param config serialized ItemStack
   * @return ItemStack iStack
   * @throws InvalidConfigurationException when failed deserialize config
   */
  @Nullable
  public static ItemStack deserialize(@NotNull String config) throws InvalidConfigurationException {
    DumperOptions yamlOptions = new DumperOptions();
    yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    yamlOptions.setIndent(2);
    Yaml yaml = new Yaml(yamlOptions);
    YamlConfiguration yamlConfiguration = new YamlConfiguration();
    Map<Object, Object> root = yaml.load(config);
    // noinspection unchecked
    Map<String, Object> item = (Map<String, Object>) root.get("item");
    int itemDataVersion = Integer.parseInt(String.valueOf(item.get("v")));
    try {
      // Try load the itemDataVersion to do some checks.
      // noinspection deprecation
      if (itemDataVersion > Bukkit.getUnsafe().getDataVersion()) {
        Util.debug("WARNING: DataVersion not matched with ItemStack: " + config);
        // okay we need some things to do
        if (BaseConfig.forceLoadDowngradeItems) {
          // okay it enabled
          Util.debug("QuickShop is trying force loading " + config);
          if (BaseConfig.forceLoadDowngradeItemsMethod == 0) { // Mode 0
            // noinspection deprecation
            item.put("v", Bukkit.getUnsafe().getDataVersion() - 1);
          } else { // Mode other
            // noinspection deprecation
            item.put("v", Bukkit.getUnsafe().getDataVersion());
          }
          // Okay we have hacked the dataVersion, now put it back
          root.put("item", item);
          config = yaml.dump(root);

          Util.debug("Updated, we will try load as hacked ItemStack: " + config);
        } else {
          ShopLogger.instance().warning("Cannot load ItemStack " + config
              + " because it saved from higher Minecraft server version, the action will fail and you will receive a exception, PLELASE DON'T REPORT TO QUICKSHOP!");
          ShopLogger.instance().warning(
              "You can try force load this ItemStack by our hacked ItemStack read util(shop.force-load-downgrade-items), but beware, the data may damaged if you load on this lower Minecraft server version, Please backup your world and database before enable!");
        }
      }
      yamlConfiguration.loadFromString(config);
      return yamlConfiguration.getItemStack("item");
    } catch (Exception e) {
      e.printStackTrace();
      yamlConfiguration.loadFromString(config);
      return yamlConfiguration.getItemStack("item");
    }
  }

  /**
   * First uppercase for every words the first char for a text.
   *
   * @param string text
   * @return Processed text.
   */
  public static String firstUppercase(@NotNull String string) {
    if (string.length() > 1) {
      return Character.toUpperCase(string.charAt(0)) + string.substring(1).toLowerCase();
    } else {
      return string.toUpperCase();
    }
  }

  /**
   * Formats the given number according to how vault would like it. E.g. $50 or 5 dollars.
   *
   * @param n price
   * @return The formatted string.
   */
  public static String format(double n) {
    if (BaseConfig.disableVaultFormat) {
      return BaseConfig.currencySymbol + n;
    }
    try {
      String formated = QuickShop.instance().getEconomy().format(n);
      if (formated == null || formated.isEmpty()) {
        Util.debug(
            "Use alternate-currency-symbol to formatting, Cause economy plugin returned null");
        return BaseConfig.currencySymbol + n;
      } else {
        return formated;
      }
    } catch (NumberFormatException e) {
      Util.debug("format", e.getMessage());
      Util.debug("format",
          "Use alternate-currency-symbol to formatting, Cause NumberFormatException");
      return BaseConfig.currencySymbol + n;
    }
  }

  /**
   * Fetches the block which the given sign is attached to
   *
   * @param sign The block which is attached
   * @return The block the sign is attached to
   */
  public static Optional<Block> getSignAttached(@NotNull Block sign) {
    if (!Util.isWallSign(sign.getType()))
      return Optional.empty();
    
    try {
      org.bukkit.block.data.BlockData data = sign.getBlockData();
      if (data instanceof org.bukkit.block.data.type.WallSign) {
        BlockFace opposide = ((org.bukkit.block.data.type.WallSign) data).getFacing().getOppositeFace();
        return Optional.of(sign.getRelative(opposide));
      }
    } catch (Throwable t) {
        return Optional.of(sign.getRelative(
            ((org.bukkit.material.Sign) sign.getState().getData()).getFacing().getOppositeFace()));
    }
    
    return Optional.empty();
  }

  public static String getItemStackName(@NotNull ItemStack itemStack) {
    if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
      return itemStack.getItemMeta().getDisplayName();
    
    return MsgUtil.getLocalizedName(itemStack.getType().name());
  }

  public static Class<?> getNMSClass(@Nullable String className) {
    if (className == null) {
      className = "MinecraftServer";
    }
    String name = Bukkit.getServer().getClass().getPackage().getName();
    String version = name.substring(name.lastIndexOf('.') + 1);
    try {
      return Class.forName("net.minecraft.server." + version + "." + className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return an entry with min and max prices, but null if there isn't a price restriction
   *
   * @param material mat
   * @return min, max
   */
  public static Entry<Double, Double> getPriceRestriction(@NotNull Material material) {
    return restrictedPrices.get(material);
  }

  /**
   * Returns the chest attached to the given chest. The given block must be a chest.
   *
   * @param chest The chest to check.
   * @return the block which is also a chest and connected to b.
   */
  public static Optional<Location> getSecondHalf(@NotNull Block chest) {
    if (!isDoubleChest(chest))
      return Optional.empty();
    
    Chest halfChest = (Chest) chest.getState();
    DoubleChestInventory chestHolder = (@Nullable DoubleChestInventory) halfChest.getInventory();
    
    Inventory right = chestHolder.getRightSide();
    Location rightLoc = right.getLocation();
    return
        Optional.of(
            (rightLoc.getX() == halfChest.getX() && rightLoc.getZ() == halfChest.getZ() ?
            chestHolder.getLeftSide().getLocation() : rightLoc));
  }

  /**
   * Gets the percentage (Without trailing %) damage on a tool.
   *
   * @param item The ItemStack of tools to check
   * @return The percentage 'health' the tool has. (Opposite of total damage)
   */
  public static String getToolPercentage(@NotNull ItemStack item) {
    if (!(item.getItemMeta() instanceof Damageable)) {
      Util.debug(item.getType().name() + " not Damageable.");
      return "Error: NaN";
    }
    double dura = ((Damageable) item.getItemMeta()).getDamage();
    double max = item.getType().getMaxDurability();
    DecimalFormat formatter = new DecimalFormat("0");
    return formatter.format((1 - dura / max) * 100.0);
  }

  /** Initialize the Util tools. */
  public static void loadFromConfig() {
    blacklist.clear();
    blockListedBlocks.clear();
    restrictedPrices.clear();
    worldBlacklist.clear();

    for (String s : BaseConfig.blacklist) {
      Material mat = Material.matchMaterial(s.toUpperCase());
      if (mat == null) {
        mat = Material.matchMaterial(s);
      }
      if (mat == null) {
        ShopLogger.instance().warning("Invalid shop-block: " + s);
      } else {
        blockListedBlocks.add(mat);
      }
    }
    List<String> configBlacklist = BaseConfig.blacklist;
    for (String s : configBlacklist) {
      Material mat = Material.getMaterial(s.toUpperCase());
      if (mat == null) {
        mat = Material.matchMaterial(s);
      }
      if (mat == null) {
        ShopLogger.instance().warning(s + " is not a valid material.  Check your spelling or ID");
        continue;
      }
      blacklist.add(mat);
    }

    for (String s : BaseConfig.priceRestriction) {
      String[] sp = s.split(";");
      if (sp.length == 3) {
        try {
          Material mat = Material.matchMaterial(sp[0]);
          if (mat == null) {
            ShopLogger.instance().warning("Material " + sp[0]
                + " in config.yml can't match with a valid Materials, check your config.yml!");
            continue;
          }
          restrictedPrices.put(mat,
              new SimpleEntry<>(Double.valueOf(sp[1]), Double.valueOf(sp[2])));
        } catch (Exception e) {
          ShopLogger.instance().warning("Invalid price restricted material: " + s);
        }
      }
    }
    worldBlacklist = BaseConfig.blacklistWorld;
  }

  /**
   * Read the InputStream to the byte array.
   *
   * @param filePath Target file
   * @return Byte array
   */
  @Nullable
  public static byte[] inputStream2ByteArray(@NotNull String filePath) {
    try {
      InputStream in = new FileInputStream(filePath);
      byte[] data = toByteArray(in);
      in.close();
      return data;
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Call this to check items in inventory and remove it.
   *
   * @param inv inv
   */
  public static void inventoryCheck(@Nullable Inventory inv) {
    if (inv == null) {
      return;
    }
    if (inv.getHolder() == null) {
      Util.debug("Skipped plugin gui inventory check.");
      return;
    }
    try {
      for (int i = 0; i < inv.getSize(); i++) {
        ItemStack itemStack = inv.getItem(i);
        if (itemStack == null) {
          continue;
        }
        if (DisplayItem.isDisplayItem(itemStack, null)) {
          // Found Item and remove it.
          Location location = inv.getLocation();
          if (location == null) {
            return; // Virtual GUI
          }
          inv.setItem(i, new ItemStack(Material.AIR));
          Util.debug("Found a displayitem in an inventory, Scheduling to removal...");
          MsgUtil.sendGlobalAlert("[InventoryCheck] Found displayItem in inventory at "
              + location + ", Item is " + itemStack.getType().name());
        }
      }
    } catch (Throwable t) {
      // Ignore
    }
  }

  public static boolean isAir(@NotNull Material mat) {
    if (mat == Material.AIR) {
      return true;
    }
    /* For 1.13 new AIR */
    try {
      if (mat == Material.CAVE_AIR) {
        return true;
      }
      if (mat == Material.VOID_AIR) {
        return true;
      }
    } catch (Throwable t) {
      // ignore
    }
    return false;
  }

  public static boolean isBlacklistWorld(@NotNull World world) {
    return worldBlacklist.contains(world.getName());
  }

  /**
   * @param stack The ItemStack to check if it is blacklisted
   * @return true if the ItemStack is black listed. False if not.
   */
  public static boolean isBlacklisted(@NotNull ItemStack stack) {
    if (blacklist.contains(stack.getType())) {
      return true;
    }
    if (!stack.hasItemMeta()) {
      return false;
    }
    if (!Objects.requireNonNull(stack.getItemMeta()).hasLore()) {
      return false;
    }
    for (String lore : Objects.requireNonNull(stack.getItemMeta().getLore())) {
      if (BaseConfig.blacklistLores.contains(lore)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get this class available or not
   *
   * @param qualifiedName class qualifiedName
   * @return boolean Available
   */
  public static boolean isClassAvailable(@NotNull String qualifiedName) {
    try {
      Class.forName(qualifiedName);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static boolean hasSpaceForDisplay(@NotNull Material mat) {
    return isAir(mat) || isWallSign(mat);
  }

  public static boolean isDoubleChest(@Nullable Block b) {
    if (b == null) {
      return false;
    }
    if (!(b.getState() instanceof Container)) {
      return false;
    }
    Container container = (Container) b.getState();
    return (container.getInventory() instanceof DoubleChestInventory);
  }

  /**
   * Returns true if the given location is loaded or not.
   *
   * @param location The location
   * @return true if the given location is loaded or not.
   */
  public static boolean isChunkLoaded(@NotNull Location location) {
    int chunkX = location.getBlockX() >> 4;
    int chunkZ = location.getBlockZ() >> 4;
    
    return location.getWorld().isChunkLoaded(chunkX, chunkZ);
  }
  
  public static boolean isChunkLoaded(@NotNull ShopLocation location) {
    int chunkX = location.x() >> 4;
    int chunkZ = location.z() >> 4;
    
    return location.world().isChunkLoaded(chunkX, chunkZ);
  }

  /**
   * Checks whether someone else's shop is within reach of a hopper being placed by a player.
   *
   * @param b The block being placed.
   * @param p The player performing the action.
   * @return true if a nearby shop was found, false otherwise.
   */
  public static boolean isOtherShopWithinHopperReach(@NotNull Block b, @NotNull Player p) {
    // Check 5 relative positions that can be affected by a hopper: behind, in front of, to the
    // right,
    // to the left and underneath.
    Block[] blocks = new Block[5];
    blocks[0] = b.getRelative(0, 0, -1);
    blocks[1] = b.getRelative(0, 0, 1);
    blocks[2] = b.getRelative(1, 0, 0);
    blocks[3] = b.getRelative(-1, 0, 0);
    blocks[4] = b.getRelative(0, 1, 0);
    for (Block c : blocks) {
      ShopViewer firstShop = ShopManager.instance().getLoadedShopAt(c.getLocation());
      // If firstShop is null but is container, it can be used to drain contents from a shop created
      // on secondHalf.
      Optional<Location> secondHalf = getSecondHalf(c);
      ShopViewer secondShop =
          secondHalf.isPresent() ?
              ShopManager.instance().getLoadedShopAt(secondHalf.get()) : ShopViewer.empty();
      
      if (firstShop.isPresent() && !p.getUniqueId().equals(firstShop.get().getOwner())
          || secondShop.isPresent() && !p.getUniqueId().equals(secondShop.get().getOwner())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check a material is possible become a shop
   *
   * @param material Mat
   * @return Can or not
   */
  public static boolean isBlocklisted(@NotNull Material material) {
    return blockListedBlocks.contains(material);
  }

  /**
   * @param mat The material to check
   * @return Returns true if the item is a tool (Has durability) or false if it doesn't.
   */
  public static boolean hasDurability(@NotNull Material mat) {
    return !(mat.getMaxDurability() == 0);
  }

  /**
   * Check a string is or not a UUID string
   *
   * @param string Target string
   * @return is UUID
   */
  public static boolean isUUID(@NotNull String string) {
    if (string.length() != 36 && string.length() != 32) {
      return false;
    }
    try {
      // noinspection ResultOfMethodCallIgnored
      UUID.fromString(string);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Check a material is or not a WALL_SIGN
   *
   * @param material mat
   * @return is or not a wall_sign
   */
  public static boolean isWallSign(@Nullable Material material) {
    if (material == null) {
      return false;
    }
    try {
      return Tag.WALL_SIGNS.isTagged(material);
    } catch (NoSuchFieldError e) {
      return "WALL_SIGN".equals(material.name());
    }
  }

  /**
   * Convert strList to String. E.g "Foo, Bar"
   *
   * @param strList Target list
   * @return str
   */
  public static String list2String(@NotNull List<String> strList) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < strList.size(); i++) {
      builder.append(strList.get(i));
      if (i + 1 != strList.size()) {
        builder.append(", ");
      }
    }
    return builder.toString();
  }

  /**
   * Returns loc with modified pitch/yaw angles so it faces lookat
   *
   * @param loc The location a players head is
   * @param lookat The location they should be looking
   * @return The location the player should be facing to have their crosshairs on the location
   *         lookAt Kudos to bergerkiller for most of this function
   */
  public static Location lookAt(Location loc, Location lookat) {
    // Clone the loc to prevent applied changes to the input loc
    loc = loc.clone();
    // Values of change in distance (make it relative)
    double dx = lookat.getX() - loc.getX();
    double dy = lookat.getY() - loc.getY();
    double dz = lookat.getZ() - loc.getZ();
    // Set yaw
    if (dx != 0) {
      // Set yaw start value based on dx
      if (dx < 0) {
        loc.setYaw((float) (1.5 * Math.PI));
      } else {
        loc.setYaw((float) (0.5 * Math.PI));
      }
      loc.setYaw(loc.getYaw() - (float) Math.atan(dz / dx));
    } else if (dz < 0) {
      loc.setYaw((float) Math.PI);
    }
    // Get the distance from dx/dz
    double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));
    float pitch = (float) -Math.atan(dy / dxz);
    // Set values, convert to degrees
    // Minecraft yaw (vertical) angles are inverted (negative)
    loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI + 360);
    // But pitch angles are normal
    loc.setPitch(pitch * 180f / (float) Math.PI);
    return loc;
  }

  /**
   * Parse colors for the YamlConfiguration.
   *
   * @param config yaml config
   */
  public static void parseColours(@NotNull YamlConfiguration config) {
    Set<String> keys = config.getKeys(true);
    for (String key : keys) {
      String filtered = config.getString(key);
      if (filtered == null) {
        continue;
      }
      if (filtered.startsWith("MemorySection")) {
        continue;
      }
      filtered = parseColours(filtered);
      config.set(key, filtered);
    }
  }

  /**
   * Parse colors for the Text.
   *
   * @param text the text
   * @return parsed text
   */
  public static String parseColours(@NotNull String text) {
    text = ChatColor.translateAlternateColorCodes('&', text);
    return text;
  }

  /**
   * Parse colors for the List.
   *
   * @param list the list
   * @return parsed list
   */
  public static List<String> parseColours(@NotNull List<String> list) {
    final List<String> newList = new ArrayList<>();
    list.forEach(s -> newList.add(parseColours(s)));

    return newList;
  }

  /**
   * Converts a name like IRON_INGOT into Iron Ingot to improve readability
   *
   * @param ugly The string such as IRON_INGOT
   * @return A nicer version, such as Iron Ingot
   */
  public static String prettifyText(@NotNull String ugly) {
    String[] nameParts = ugly.split("_");
    if (nameParts.length == 1) {
      return firstUppercase(ugly);
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < nameParts.length; i++) {
      sb.append(firstUppercase(nameParts[i]));
      if (i + 1 != nameParts.length) {
        sb.append(" ");
      }
    }
    return sb.toString();
  }

  /**
   * Read the file to the String
   *
   * @param fileName Target file.
   * @return Target file's content.
   */
  public static String readToString(@NotNull String fileName) {
    File file = new File(fileName);
    long filelength = file.length();
    byte[] filecontent = new byte[(int) filelength];
    try {
      FileInputStream in = new FileInputStream(file);
      in.read(filecontent);
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new String(filecontent, StandardCharsets.UTF_8);
  }

  /**
   * Read the file to the String
   *
   * @param file Target file.
   * @return Target file's content.
   */
  public static String readToString(@NotNull File file) {
    long filelength = file.length();
    byte[] filecontent = new byte[(int) filelength];
    try {
      FileInputStream in = new FileInputStream(file);
      in.read(filecontent);
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new String(filecontent, StandardCharsets.UTF_8);
  }

  /**
   * Covert ItemStack to YAML string.
   *
   * @param iStack target ItemStack
   * @return String serialized itemStack
   */
  public static String serialize(@NotNull ItemStack iStack) { // FIXME move
    YamlConfiguration cfg = new YamlConfiguration();
    cfg.set("item", iStack);
    return cfg.saveToString();
  }

  private static byte[] toByteArray(@NotNull InputStream in) throws IOException {

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024 * 4];
    int n;
    while ((n = in.read(buffer)) != -1) {
      out.write(buffer, 0, n);
    }
    return out.toByteArray();
  }

  public static String getNMSVersion() {
    String name = Bukkit.getServer().getClass().getPackage().getName();
    return name.substring(name.lastIndexOf('.') + 1);
  }

  /**
   * Get the sign material using by QuickShop.instance(). With compatiabily process.
   *
   * @return The material now using.
   */
  public static Material getSignMaterial() { // FIXME cache

    Material signMaterial = Material.matchMaterial(BaseConfig.signMaterial);
    if (signMaterial != null && signMaterial.name().endsWith("WALL_SIGN")) {
      return signMaterial;
    }
    signMaterial = Material.matchMaterial("OAK_WALL_SIGN"); // Fallback default sign in 1.14
    if (signMaterial != null) {
      return signMaterial;
    }
    signMaterial = Material.matchMaterial("WALL_SIGN"); // Fallback default sign in 1.13
    if (signMaterial != null) {
      return signMaterial;
    }
    // What the fuck!?
    ShopLogger.instance().warning(
        "QuickShop can't found any useable sign material, we will use default Sign Material.");
    try {
      return Material.OAK_WALL_SIGN;
    } catch (Throwable e) {
      return Material.valueOf("WALL_SIGN");
    }
  }

  /**
   * Get MinecraftServer's TPS
   *
   * @return TPS (e.g 19.92)
   */
  public static Double getTPS() {
    try {
      serverInstance = getNMSClass("MinecraftServer").getMethod("getServer").invoke(null);
      tpsField = serverInstance.getClass().getField("recentTps");
    } catch (NoSuchFieldException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
      e.printStackTrace();
    }
    try {
      double[] tps = ((double[]) tpsField.get(serverInstance));
      return tps[0];
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Check QuickShop is running on dev edition or not.
   *
   * @return DevEdition status
   */
  public static boolean isDevEdition(String pluginVersion) {
    String version = pluginVersion.toLowerCase();
    return (version.contains("dev") || version.contains("develop") || version.contains("alpha")
        || version.contains("beta") || version.contains("test") || version.contains("snapshot")
        || version.contains("preview"));
  }

  /**
   * Get the plugin is under dev-mode(debug mode)
   *
   * @return under dev-mode
   */
  public static boolean isDevMode() {
    return true; // FIXME
  }

  /**
   * Get a material is a dye
   *
   * @param material The material
   * @return yes or not
   */
  public static boolean isDyes(@Nullable Material material) {
    return material.name().endsWith("_DYE");
  }

  /**
   * Call a event and check it is cancelled.
   *
   * @param event The event implement the Cancellable interface.
   * @return The event is cancelled.
   */
  public static boolean fireCancellableEvent(@NotNull Cancellable event) {
    Bukkit.getPluginManager().callEvent((Event) event);
    return event.isCancelled();
  }

  /**
   * Get QuickShop caching folder
   *
   * @return The caching folder
   */
  public static File getCacheFolder() {
    File cache = new File(QuickShop.instance().getDataFolder(), "cache");
    if (!cache.exists())
      cache.mkdirs();
    return cache;
  }
}
