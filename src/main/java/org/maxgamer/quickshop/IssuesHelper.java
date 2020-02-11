package org.maxgamer.quickshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import cc.bukkit.shop.util.ShopLogger;

public class IssuesHelper {
  public static IssuesHelper create() {
    return new IssuesHelper();
  }
  
  private boolean errored;
  
  public boolean hasErrored() {
    return errored;
  }
  
  public boolean scanDupeInstall() {
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

  protected void error(@NotNull String... errors) {
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
  public void databaseError() {
    error("Error connecting to the database",
        "Make sure your database service is running.",
        "Or check the configuration in your config.yml");
  }

  /**
   * Call when failed load economy system, and use this to check the reason.
   *
   * @return The reason of error.
   */
  public void econError() {
    // Check Vault is installed
    if (Bukkit.getPluginManager().getPlugin("Vault") == null
        && Bukkit.getPluginManager().getPlugin("Reserve") == null) {
      // Vault not installed
      error("Vault or Reserve is not installed or loaded!",
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
      error(
          "No Economy plugin detected, did you installed and loaded them? Make sure they loaded before QuickShop.",
          "Make sure you have an economy plugin hooked into Vault or Reserve.",
          ChatColor.YELLOW + "Incompatibility detected: CMI Installed",
          "Download CMI Edition of Vault might fix this.");
    }

    error(
        "No Economy plugin detected, did you installed and loaded them? Make sure they loaded before QuickShop.",
        "Install an economy plugin to get Vault or Reserve working.");
  }
}
