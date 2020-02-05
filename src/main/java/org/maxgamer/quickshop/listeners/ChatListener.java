package org.maxgamer.quickshop.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.utils.Util;

public class ChatListener implements Listener {
  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncPlayerChatEvent e) {

    if (e.isCancelled() && BaseConfig.ignoreChatCancelling) {
      Util.debugLog("Ignored a chat event (Canceled by another plugin.)");
      return;
    }

    if (!ShopManager.instance().getActions().containsKey(e.getPlayer().getUniqueId())) {
      return;
    }
    // Fix stupid chat plugin will add a weird space before or after the number we want.
    ShopManager.instance().handleChat(e.getPlayer(), e.getMessage().trim());
    e.setCancelled(true);
  }
}
