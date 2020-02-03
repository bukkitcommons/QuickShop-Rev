package org.maxgamer.quickshop.command;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.CommandProcesser;

public abstract class SneakyTabs implements CommandProcesser {
  @NotNull
  @Override
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    return Collections.emptyList();
  }
}