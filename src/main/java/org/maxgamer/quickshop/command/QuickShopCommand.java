package org.maxgamer.quickshop.command;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import cc.bukkit.shop.command.ShopCommand;

public abstract class QuickShopCommand implements ShopCommand {
  @Nullable
  private String label;
  
  @Override
  public String label() {
    // This will strip Command as the prefix
    return label == null ? label = getClass().getName().substring(7).toLowerCase() : label;
  }
  
  @Override
  public List<String> permissions() {
    return Collections.emptyList();
  }
}
