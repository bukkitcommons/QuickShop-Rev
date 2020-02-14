package org.maxgamer.quickshop.utils;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import com.google.common.io.Files;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopLocation;
import cc.bukkit.shop.database.connector.MySQLConnector;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.viewer.ShopViewer;
import lombok.Getter;

public class Util {
  private static EnumSet<Material> blacklist = EnumSet.noneOf(Material.class);
  @Getter
  private static List<String> debugLogs = new LinkedList<>();
  private static EnumMap<Material, Entry<Double, Double>> restrictedPrices =
      new EnumMap<>(Material.class);
  private static EnumSet<Material> blockListedBlocks = EnumSet.noneOf(Material.class);
  private static List<String> worldBlacklist = new ArrayList<>();

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
  
  /**
   * Print debug log when plugin running on dev mode.
   *
   * @param logs logs
   */
  public static void debug(@NotNull String... logs) {
    if (!BaseConfig.developerMode)
      return;
    
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

    int codeLine = stackTraceElement.getLineNumber();
    for (String log : logs) {
      String text = "["
          + ChatColor.DARK_GREEN + className + ChatColor.RESET + "] ("
          + ChatColor.DARK_GREEN + codeLine + ChatColor.RESET + ") " + log;
      debugLogs.add(Colorizer.stripColors(text));
      if (debugLogs.size() > 500000) /* Keep debugLogs max can have 500k lines. */ {
        debugLogs.remove(0);
      }
      ShopLogger.instance().info(text);
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
      ShopViewer firstShop = Shop.getManager().getLoadedShopAt(c.getLocation());
      // If firstShop is null but is container, it can be used to drain contents from a shop created
      // on secondHalf.
      Optional<Location> secondHalf = BlockUtils.getSecondHalf(c);
      ShopViewer secondShop =
          secondHalf.isPresent() ?
              Shop.getManager().getLoadedShopAt(secondHalf.get()) : ShopViewer.empty();
      
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
   * @param fileConfiguration yaml config
   */
  public static void parseColours(@NotNull FileConfiguration fileConfiguration) {
    Set<String> keys = fileConfiguration.getKeys(true);
    for (String key : keys) {
      String filtered = fileConfiguration.getString(key);
      if (filtered == null) {
        continue;
      }
      if (filtered.startsWith("MemorySection")) {
        continue;
      }
      filtered = parseColours(filtered);
      fileConfiguration.set(key, filtered);
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

  private final static YamlConfiguration SERIALIZER = new YamlConfiguration();

  /**
   * Covert ItemStack to YAML string.
   *
   * @param iStack target ItemStack
   * @return String serialized itemStack
   */
  public static String serializeItem(@NotNull ItemStack iStack) {
    SERIALIZER.set("item", iStack);
    return SERIALIZER.saveToString();
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
   * Call a event and check it is cancelled.
   *
   * @param event The event implement the Cancellable interface.
   * @return The event is cancelled.
   */
  public static boolean fireCancellableEvent(@NotNull Cancellable event) {
    Bukkit.getPluginManager().callEvent((Event) event);
    return event.isCancelled();
  }

  public static boolean isChunkLoaded(@NotNull World world, int chunkX, int chunkZ) {
    return world.isChunkLoaded(chunkX, chunkZ);
  }
}
