package org.maxgamer.quickshop.command.sub;

import java.io.IOException;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.VersionData;
import org.maxgamer.quickshop.utils.VersionUpdater;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.command.SneakyTabs;

public class SubCommand_Update extends SneakyTabs implements CommandProcesser {
  @Override
  public void onCommand(
      @NotNull CommandSender sender,
      @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    
    Bukkit.getScheduler().runTaskAsynchronously(QuickShop.instance(), () -> {
      sender.sendMessage(ChatColor.YELLOW + "Checking for updates...");

      final Optional<VersionData> data = VersionUpdater.acquire();
      if (!data.isPresent()) {
        sender.sendMessage(ChatColor.GREEN + "No updates can update now.");
        return;
      }

      sender.sendMessage(ChatColor.YELLOW + "Downloading update, this may need a while...");
      final byte[] pluginBin;
      
      try {
        pluginBin = VersionUpdater.downloadUpdatedJar();
      } catch (IOException e) {
        sender.sendMessage(ChatColor.RED + "Update failed, get details to look the console.");
        QuickShop.instance().getSentryErrorReporter().ignoreThrow();
        e.printStackTrace();
        return;
      }
      
      if (pluginBin.length < 1) {
        sender.sendMessage(
            ChatColor.RED + "Download failed, check your connection before contact the author.");
        return;
      }
      
      sender.sendMessage(ChatColor.YELLOW + "Installing update...");
      
      try {
        VersionUpdater.replaceTheJar(pluginBin);
      } catch (IOException ioe) {
        sender.sendMessage(ChatColor.RED + "Update failed, get details to look the console.");
        QuickShop.instance().getSentryErrorReporter().ignoreThrow();
        ioe.printStackTrace();
        return;
      } catch (RuntimeException re) {
        sender.sendMessage(ChatColor.RED + "Update failed, " + re.getMessage());
        return;
      }

      sender.sendMessage(
          ChatColor.GREEN + "Successfully, restart your server to apply the changes!");
    });
  }
}
