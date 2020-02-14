package org.maxgamer.quickshop.scheduler;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.utils.JavaUtils;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.util.version.VersionData;
import cc.bukkit.shop.util.version.VersionFetcher;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class UpdateWatcher implements Listener {
  private volatile static Optional<VersionData> data = Optional.empty();
  private static int taskId = 1;
  
  public static final VersionFetcher FETCHER = VersionFetcher.create("66351");
  
  public static boolean isLatest() {
    return !data.isPresent();
  }

  public static void init() {
    taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(QuickShop.instance(), () -> {
      
      (data = FETCHER.acquire()).ifPresent(data -> {
        if (!data.beta()) {
          ShopLogger.instance()
              .info("A new version of QuickShop has been released! [" + data.version() + "]");
          ShopLogger.instance()
              .info("Update here: https://www.spigotmc.org/resources/62575/");
          Bukkit.getOnlinePlayers().forEach(player -> alert(player));
        }
      });
      
    }, 0, 15 * 60 * 20).getTaskId();
  }

  public static void uninit() {
    if (taskId != -1)
      Bukkit.getScheduler().cancelTask(taskId);
  }
  
  private static void alert(Player sender) {
    data.ifPresent(data -> {
      if (!data.beta()) {
        
        ShopLogger.instance()
            .info("A new version of QuickShop has been released! [" + data.version() + "]");
        ShopLogger.instance()
            .info("Update here: https://www.spigotmc.org/resources/62575/");
        
        String notify = JavaUtils.fillArgs(
            "New update {0} now avaliable! Please update!",
            data.version(), Shop.getVersion()
        );
        
        TextComponent updatenow = new TextComponent(
            ChatColor.AQUA + Shop.getLocaleManager().get("updatenotify.buttontitle", sender.getName()));
        TextComponent onekeyupdate = new TextComponent(
            ChatColor.YELLOW + Shop.getLocaleManager().get("updatenotify.onekeybuttontitle", sender.getName()));
        updatenow.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
            "https://www.spigotmc.org/resources/62575/"));
        onekeyupdate
            .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/qs update"));
        TextComponent finallyText =
            new TextComponent(updatenow, new TextComponent(" "), onekeyupdate);
        sender.sendMessage(
            ChatColor.GREEN + "---------------------------------------------------");
        sender.sendMessage(ChatColor.GREEN + notify);
        sender.spigot().sendMessage(finallyText);
        sender.sendMessage(
            ChatColor.GREEN + "---------------------------------------------------");
        
      } else {
        ShopLogger.instance().info("A new BETA version of QuickShop is available!");
        ShopLogger.instance()
            .info("Update here: https://www.spigotmc.org/resources/62575/");
        ShopLogger.instance()
            .info("This is a BETA version, which means you should use it with caution.");
      }
    });
  }
  
  @EventHandler
  public void playerJoin(PlayerJoinEvent e) {
    if (QuickShopPermissionManager.instance().has(e.getPlayer(), "quickshop.alert"))
      Bukkit.getScheduler().runTaskLater(QuickShop.instance(), () -> alert(e.getPlayer()), 4 * 20);
  }
}
