package org.maxgamer.quickshop.permission;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public interface PermissionProvider {
  /**
   * Test the sender has special permission
   *
   * @param sender CommandSender
   * @param permission The permission want to check
   * @return hasPermission
   */
  boolean hasPermission(@NotNull CommandSender sender, @NotNull String permission);

  /**
   * Get permission provider name
   *
   * @return The name of permission provider
   */
  @NotNull
  String getName();
}
