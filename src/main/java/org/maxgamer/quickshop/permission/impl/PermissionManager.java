package org.maxgamer.quickshop.permission.impl;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.permission.PermissionProvider;
import org.maxgamer.quickshop.permission.ProviderType;

public class PermissionManager {
  private final PermissionProvider provider;

  /**
   * The manager to call permission providers
   *
   * @param plugin Instance
   */
  public PermissionManager() {
    switch (ProviderType.valueOf(BaseConfig.permsProvider)) {
      case VAULT:
        provider = new VaultPermsProvider();
        break;
      case BUKKIT:
      default:
        provider = new BukkitPermsProvider();
    }
    
    QuickShop.instance().getLogger().info("Selected permission provider: " + provider.getName());
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
