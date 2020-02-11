package org.maxgamer.quickshop.command;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import cc.bukkit.shop.command.ShopCommand;

public abstract class QuickShopCommand implements ShopCommand {
  @Nullable
  private String label;
  
  @Override
  public String label() {
    // This will strip Command as the prefix
    return label == null ? label = getClass().getSimpleName().substring(7).toLowerCase() : label;
  }
  
  @NotNull
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
    return Collections.emptyList();
  }
  
  @Override
  public List<String> permissions() {
    return Collections.emptyList();
  }
}
