package org.maxgamer.quickshop.permission.impl;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.permission.PermissionProvider;
import org.maxgamer.quickshop.permission.ProviderType;

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
