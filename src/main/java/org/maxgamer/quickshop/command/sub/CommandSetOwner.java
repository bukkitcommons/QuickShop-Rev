package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;

import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandSetOwner extends QuickShopCommand {
    @Override
    public List<String> permissions() {
        return Collections.singletonList("quickshop.setowner");
    }
    
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Shop.getLocaleManager().get("Only player can run this command"));
            return;
        }
        
        if (cmdArg.length < 1) {
            sender.sendMessage(Shop.getLocaleManager().get("command.no-owner-given"));
            return;
        }
        
        final BlockIterator bIt = new BlockIterator((Player) sender, 10);
        
        if (!bIt.hasNext()) {
            sender.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop"));
            return;
        }
        
        while (bIt.hasNext()) {
            final Block b = bIt.next();
            final ShopViewer shop = Shop.getManager().getLoadedShopAt(b.getLocation());
            
            if (!shop.isPresent())
                continue;
            
            @SuppressWarnings("deprecation")
            final OfflinePlayer p = Bukkit.getOfflinePlayer(cmdArg[0]);
            final String shopOwner = Bukkit.getOfflinePlayer(shop.get().getOwner()).getName();
            if (!p.hasPlayedBefore()) {
                sender.sendMessage(Shop.getLocaleManager().get("unknown-player"));
                return;
            }
            shop.get().setOwner(p.getUniqueId());
            shop.<ChestShop>get().setSignText();
            Shop.getManager().save(shop.get());
            sender.sendMessage(Shop.getLocaleManager().get("command.new-owner", shopOwner));
            return;
        }
        
        sender.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop"));
    }
}
