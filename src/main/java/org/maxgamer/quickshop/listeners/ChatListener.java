package org.maxgamer.quickshop.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.maxgamer.quickshop.configuration.BaseConfig;
import cc.bukkit.shop.Shop;

public class ChatListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() && BaseConfig.ignoreChatCancelling)
            return;
        
        if (!Shop.getActions().hasAction(event.getPlayer().getUniqueId()))
            return;
        
        // Fix stupid chat plugin will add a weird space before or after the number we
        // want.
        Shop.getActions().handleChat(event.getPlayer(), event.getMessage().trim(), true);
        event.setCancelled(true);
    }
}
