package org.maxgamer.quickshop.utils.messages;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Maps;
import cc.bukkit.shop.util.ShopLogger;

public class ShopMessager {
  @NotNull
  private static HashMap<UUID, String> playerMessages = Maps.newHashMap();

  /** Deletes any messages that are older than a week in the database, to save on space. */
  public static void clean() {
    ShopLogger.instance()
        .info("Cleaning purchase messages from the database that are over a week old...");
    // 604800,000 msec = 1 week.
    long weekAgo = System.currentTimeMillis() - 604800000;
    QuickShop.instance().getDatabaseHelper().cleanMessage(weekAgo);
  }

  /**
   * Replace args in raw to args
   *
   * @param raw text
   * @param args args
   * @return filled text
   */
  public static String fillArgs(@Nullable String raw, @Nullable String... args) {
    if (raw == null) {
      return "Invalid message: null";
    }
    if (raw.isEmpty()) {
      return "";
    }
    if (args == null) {
      return raw;
    }
    for (int i = 0; i < args.length; i++) {
      raw = StringUtils.replace(raw, "{" + i + "}", args[i] == null ? "" : args[i]);
    }
    return raw;
  }

  /**
   * Empties the queue of messages a player has and sends them to the player.
   *
   * @param p The player to message
   * @return True if success, False if the player is offline or null
   */
  public static void flushMessagesFor(@NotNull Player player) {
    UUID uuid = player.getUniqueId();
    String message = playerMessages.remove(uuid);
    
    if (message != null) {
      MsgUtil.sendMessage(player, message);
      QuickShop.instance().getDatabaseHelper().cleanMessageForPlayer(uuid);
    }
  }
  
  /**
   * @param player The name of the player to message
   * @param message The message to send them Sends the given player a message if they're online.
   *        Else, if they're not online, queues it for them in the database.
   * @param isUnlimited The shop is or unlimited
   */
  public static void send(@NotNull UUID uuid, @NotNull String message) {
    Player player = Bukkit.getPlayer(uuid);
    if (player == null) {
      playerMessages.put(uuid, message);
      QuickShop.instance().getDatabaseHelper().sendMessage(uuid, message, System.currentTimeMillis());
    } else {
      MsgUtil.sendMessage(player, message);
    }
  }
  
  /** loads all player purchase messages from the database. */
  public static void loadTransactionMessages() {
    playerMessages.clear(); // Delete old messages
    
    try {
      ResultSet rs = QuickShop.instance().getDatabaseHelper().selectAllMessages();
      
      while (rs.next()) {
        String owner = rs.getString("owner");
        UUID ownerUUID;
        if (Util.isUUID(owner)) {
          ownerUUID = UUID.fromString(owner);
        } else {
          ownerUUID = Bukkit.getOfflinePlayer(owner).getUniqueId();
        }
        
        playerMessages.put(ownerUUID, rs.getString("message"));
      }
      
    } catch (Throwable t) {
      ShopLogger.instance().severe("Could not load transaction messages from database.");
      t.printStackTrace();
    }
  }
}