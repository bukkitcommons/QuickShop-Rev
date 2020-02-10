package org.maxgamer.quickshop.permission;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import cc.bukkit.shop.permission.PermissionProvider;
import cc.bukkit.shop.permission.ProviderType;

public class BukkitPermsProvider implements PermissionProvider {

  @Override
  public boolean hasPermission(@NotNull CommandSender sender, @NotNull String permission) {
    return sender.hasPermission(permission);
  }

  @Override
  @NotNull
  public ProviderType getType() {
    return ProviderType.BUKKIT;
  }
}
