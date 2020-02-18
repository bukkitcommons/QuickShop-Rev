package org.maxgamer.quickshop.permission;

import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.configuration.BaseConfig;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.manager.PermissionManager;
import cc.bukkit.shop.permission.PermissionProvider;
import cc.bukkit.shop.permission.provider.BukkitPermsProvider;

public class QuickShopPermissionManager implements PermissionManager {
  /*
   * Singleton
   */
  private static class LazySingleton {
    private static final QuickShopPermissionManager INSTANCE = new QuickShopPermissionManager();
  }
  
  private QuickShopPermissionManager() {
    switch (BaseConfig.permsProvider.toUpperCase(Locale.ROOT)) {
      case "VAULT":
        provider = new VaultPermsProvider();
        break;
      default:
        provider = new BukkitPermsProvider();
    }
    
    ShopLogger.instance().info("Selected permission provider: " + provider.getType());
  }
  
  public static QuickShopPermissionManager instance() {
    return LazySingleton.INSTANCE;
  }
  
  private final PermissionProvider provider;

  /*
   * Permission
   */
  @Override
  public boolean hasExact(@NotNull CommandSender sender, @NotNull String permission) {
    return provider.hasPermission(sender, permission);
  }
}
