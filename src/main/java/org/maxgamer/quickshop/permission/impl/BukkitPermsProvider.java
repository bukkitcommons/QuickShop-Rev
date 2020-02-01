package org.maxgamer.quickshop.permission.impl;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.permission.PermissionProvider;

public class BukkitPermsProvider implements PermissionProvider {

  @Override
  public boolean hasPermission(@NotNull CommandSender sender, @NotNull String permission) {
    return sender.hasPermission(permission);
  }

  @NotNull
  @Override
  public String getName() {
    return "Bukkit";
  }
}
