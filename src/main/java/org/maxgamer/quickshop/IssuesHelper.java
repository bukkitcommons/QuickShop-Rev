package org.maxgamer.quickshop;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.messages.ShopPluginLogger;

/** BootError class contains print errors on /qs command when plugin failed launched. */
@EqualsAndHashCode
@ToString
public class IssuesHelper {
  private static boolean errored;
  
  public static boolean hasErrored() {
    return errored;
  }
  
  public static boolean scanDupeInstall() {
    try {
      try {
        Class.forName("org.maxgamer.quickshop.Util.NMS");
      } catch (ClassNotFoundException e) {
        ;
      } catch (NoClassDefFoundError e) {
        errored = true;
        Bukkit.getLogger().severe(
            "You have installed an older version of QuickShop, please remove that first!");
        Bukkit.getPluginManager().disablePlugin(QuickShop.instance());
        return false;
      }
      
      try {
        Class.forName("org.maxgamer.quickshop.Watcher.LogWatcher"); // Reremake, v3
        Class.forName("org.maxgamer.quickshop.watcher.LogWatcher"); // Reremake, v4
      } catch (ClassNotFoundException e) {
        ;
      } catch (NoClassDefFoundError e) {
        errored = true;
        Bukkit.getLogger().severe(
            "You have installed another edition of QuickShop, please check your plugins!");
        Bukkit.getPluginManager().disablePlugin(QuickShop.instance());
        return false;
      }
    } catch (Throwable t) {
      // This can comes from disabling plugin.
      errored = true;
      return false;
    }
    
    return true;
  }

  protected static void error(@NotNull String... errors) {
    errored = true;
    ShopLogger.instance().severe("#####################################################");
    ShopLogger.instance().severe(" QuickShop is disabled, Please fix any errors and restart");
    for (String err : errors)
      ShopLogger.instance().severe(err);
    ShopLogger.instance().severe("#####################################################");
  }
  
  /**
   * Call when failed load database, and use this to check the reason.
   *
   * @return The reason of error.
   */
  static void databaseError() {
    IssuesHelper.error("Error connecting to the database",
        "Make sure your database service is running.",
        "Or check the configuration in your config.yml");
  }

  /**
   * Call when failed load economy system, and use this to check the reason.
   *
   * @return The reason of error.
   */
  static void econError() {
    // Check Vault is installed
    if (Bukkit.getPluginManager().getPlugin("Vault") == null
        && Bukkit.getPluginManager().getPlugin("Reserve") == null) {
      // Vault not installed
      IssuesHelper.error("Vault or Reserve is not installed or loaded!",
          "Make sure you installed Vault or Reserve.");
    }
    // if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
    // // Vault not installed
    // return new BootError("Vault is not installed or loaded!", "Make sure you installed
    // Vault.");
    // }
    // Vault is installed
    if (Bukkit.getPluginManager().getPlugin("CMI") != null) {
      // Found may in-compatiable plugin
      IssuesHelper.error(
          "No Economy plugin detected, did you installed and loaded them? Make sure they loaded before QuickShop.",
          "Make sure you have an economy plugin hooked into Vault or Reserve.",
          ChatColor.YELLOW + "Incompatibility detected: CMI Installed",
          "Download CMI Edition of Vault might fix this.");
    }

    IssuesHelper.error(
        "No Economy plugin detected, did you installed and loaded them? Make sure they loaded before QuickShop.",
        "Install an economy plugin to get Vault or Reserve working.");
  }
}
