package org.maxgamer.quickshop.permission.impl;

import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.permission.PermissionProvider;

public class PermissionManager {
  private final PermissionProvider provider;

  /**
   * The manager to call permission providers
   *
   * @param plugin Instance
   */
  public PermissionManager() {
    switch (BaseConfig.permsProvider.toUpperCase(Locale.ROOT)) {
      case "VAULT":
        provider = new VaultPermsProvider();
        break;
      default:
        provider = new BukkitPermsProvider();
    }
    
    QuickShop.instance().getLogger().info("Selected permission provider: " + provider.getType());
  }

  /**
   * Check the permission for sender
   *
   * @param sender The CommandSender you want check
   * @param permission The permission node wait to check
   * @return The result of check
   */
  public boolean hasPermission(@NotNull CommandSender sender, @NotNull String permission) {
    return sender.isOp() ? true : provider.hasPermission(sender, permission);
  }
  
  public boolean hasPermissionExactly(@NotNull CommandSender sender, @NotNull String permission) {
    return provider.hasPermission(sender, permission);
  }
}
