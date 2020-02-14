package org.maxgamer.quickshop.permission;

import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.configuration.BaseConfig;
import cc.bukkit.shop.permission.PermissionProvider;
import cc.bukkit.shop.permission.provider.BukkitPermsProvider;
import cc.bukkit.shop.util.ShopLogger;

public class PermissionManager {
  /*
   * Singleton
   */
  private static class LazySingleton {
    private static final PermissionManager INSTANCE = new PermissionManager();
  }
  
  private PermissionManager() {
    switch (BaseConfig.permsProvider.toUpperCase(Locale.ROOT)) {
      case "VAULT":
        provider = new VaultPermsProvider();
        break;
      default:
        provider = new BukkitPermsProvider();
    }
    
    ShopLogger.instance().info("Selected permission provider: " + provider.getType());
  }
  
  public static PermissionManager instance() {
    return LazySingleton.INSTANCE;
  }
  
  private final PermissionProvider provider;

  /**
   * Check the permission for sender
   *
   * @param sender The CommandSender you want check
   * @param permission The permission node wait to check
   * @return The result of check
   */
  public boolean has(@NotNull CommandSender sender, @NotNull String permission) {
    return sender.isOp() ? true : provider.hasPermission(sender, permission);
  }
  
  public boolean hasExact(@NotNull CommandSender sender, @NotNull String permission) {
    return provider.hasPermission(sender, permission);
  }
}