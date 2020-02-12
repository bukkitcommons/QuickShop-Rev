package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.VersionDataFetcher;
import cc.bukkit.shop.util.VersionData;

public class CommandUpdate extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.alert");
  }
  
  @Override
  public void onCommand(
      @NotNull CommandSender sender,
      @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    
    Bukkit.getScheduler().runTaskAsynchronously(QuickShop.instance(), () -> {
      sender.sendMessage(ChatColor.YELLOW + "Checking for updates...");

      final Optional<VersionData> data = VersionDataFetcher.acquire();
      if (!data.isPresent()) {
        sender.sendMessage(ChatColor.GREEN + "No updates can update now.");
        return;
      } else {
        VersionData versionData = data.get();
        sender.sendMessage(ChatColor.GREEN + "Found a new version: " + versionData.version() +
            " (Current " + QuickShop.instance().getVersion() + " )");
      }
    });
  }
}
