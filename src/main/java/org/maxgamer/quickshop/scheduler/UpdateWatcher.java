package org.maxgamer.quickshop.scheduler;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.utils.VersionData;
import org.maxgamer.quickshop.utils.VersionUpdater;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.messages.ShopPluginLogger;

public class UpdateWatcher implements Listener {
  private volatile static Optional<VersionData> data = Optional.empty();
  private static int taskId = 1;
  
  public static boolean isLatest() {
    return !data.isPresent();
  }

  public static void init() {
    taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(QuickShop.instance(), () -> {
      
      (data = VersionUpdater.acquire()).ifPresent(data -> {
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
  
  private static void alert(CommandSender sender) {
    data.ifPresent(data -> {
      if (!data.beta()) {
        
        ShopLogger.instance()
            .info("A new version of QuickShop has been released! [" + data.version() + "]");
        ShopLogger.instance()
            .info("Update here: https://www.spigotmc.org/resources/62575/");
        
        List<String> messages = MsgUtil.getI18nFile().getStringList("updatenotify.list");
        
        String notify = MsgUtil.fillArgs(
            messages.isEmpty() ?
                "New update {0} now avaliable! Please update!" :
                  
                  (messages.size() > 1 ?
                      messages.get(
                          ThreadLocalRandom.current().nextInt(messages.size())) :
                      messages.get(0)
                  ),
            data.version(), QuickShop.instance().getVersion()
        );
        
        TextComponent updatenow = new TextComponent(
            ChatColor.AQUA + MsgUtil.getMessage("updatenotify.buttontitle", sender));
        TextComponent onekeyupdate = new TextComponent(
            ChatColor.YELLOW + MsgUtil.getMessage("updatenotify.onekeybuttontitle", sender));
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
    if (PermissionManager.instance().has(e.getPlayer(), "quickshop.alert"))
      Bukkit.getScheduler().runTaskLater(QuickShop.instance(), () -> alert(e.getPlayer()), 4 * 20);
  }
}
