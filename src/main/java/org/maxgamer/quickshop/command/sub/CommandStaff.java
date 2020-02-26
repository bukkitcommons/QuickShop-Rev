package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandStaff extends QuickShopCommand {
    @Override
    public List<String> permissions() {
        return Collections.singletonList("quickshop.staff");
    }
    
    @NotNull
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        final ArrayList<String> tabList = new ArrayList<>();
        
        if (cmdArg.length < 2) {
            if (cmdArg.length == 1) {
                final String prefix = cmdArg[0].toLowerCase();
                
                if ("add".startsWith(prefix) || "add".equals(prefix)) {
                    tabList.add("add");
                }
                
                if ("del".startsWith(prefix) || "del".equals(prefix)) {
                    tabList.add("del");
                }
                
                if ("list".startsWith(prefix) || "list".equals(prefix)) {
                    tabList.add("list");
                }
                
                if ("clear".startsWith(prefix) || "clear".equals(prefix)) {
                    tabList.add("clear");
                }
            } else {
                tabList.add("add");
                tabList.add("del");
                tabList.add("list");
                tabList.add("clear");
            }
            
            return tabList;
        }
        
        return tabList;
    }
    
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only player can execute this command.");
            return;
        }
        
        final BlockIterator bIt = new BlockIterator((LivingEntity) sender, 10);
        
        if (!bIt.hasNext()) {
            sender.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop"));
            return;
        }
        
        while (bIt.hasNext()) {
            final Block b = bIt.next();
            final ShopViewer shop = Shop.getManager().getLoadedShopAt(b.getLocation());
            
            if (!shop.isPresent() || !shop.get().moderator().isModerator(((Player) sender).getUniqueId())) {
                continue;
            }
            
            switch (cmdArg.length) {
                case 0:
                    sender.sendMessage(Shop.getLocaleManager().get("command.wrong-args"));
                    return;
                case 1:
                    switch (cmdArg[0]) {
                        case "add":
                            sender.sendMessage(Shop.getLocaleManager().get("command.wrong-args"));
                            return;
                        case "del":
                            sender.sendMessage(Shop.getLocaleManager().get("command.wrong-args"));
                            return;
                        case "clear":
                            shop.get().clearStaffs();
                            sender.sendMessage(Shop.getLocaleManager().get("shop-staff-cleared"));
                            return;
                        case "list":
                            final Set<UUID> staffs = shop.get().getStaffs();
                            
                            if (staffs.isEmpty()) {
                                sender.sendMessage(ChatColor.GREEN + Shop.getLocaleManager().get("tableformat.left_begin") + "Empty");
                                return;
                            }
                            
                            for (UUID uuid : staffs) {
                                sender.sendMessage(ChatColor.GREEN + Shop.getLocaleManager().get("tableformat.left_begin") + Bukkit.getOfflinePlayer(uuid).getName());
                            }
                            
                            return;
                        default:
                            sender.sendMessage(Shop.getLocaleManager().get("command.wrong-args"));
                    }
                    
                    break;
                case 2:
                    final OfflinePlayer offlinePlayer = QuickShop.instance().getServer().getOfflinePlayer(cmdArg[1]);
                    String offlinePlayerName = offlinePlayer.getName();
                    
                    if (offlinePlayerName == null) {
                        offlinePlayerName = "null";
                    }
                    
                    switch (cmdArg[0]) {
                        case "add":
                            shop.get().addStaff(offlinePlayer.getUniqueId());
                            sender.sendMessage(Shop.getLocaleManager().get("shop-staff-added", offlinePlayerName));
                            return;
                        case "del":
                            sender.sendMessage(Shop.getLocaleManager().get("shop-staff-deleted", offlinePlayerName));
                            return;
                        default:
                            sender.sendMessage(Shop.getLocaleManager().get("command.wrong-args"));
                    }
                    
                    break;
                default:
                    Util.debug("No any args matched");
                    break;
            }
        }
    }
}
