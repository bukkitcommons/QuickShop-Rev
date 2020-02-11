package org.maxgamer.quickshop.command.sub;

import java.sql.SQLException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.shop.QuickShopLoader;
import org.maxgamer.quickshop.shop.QuickShopManager;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.command.SneakyTabs;

public class SubCommand_CleanGhost extends SneakyTabs implements CommandProcesser {

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (cmdArg.length < 1) {
      sender.sendMessage(ChatColor.YELLOW
          + "This command will purge all data damaged shop, create in disallow world shop, create disallow sell items shop and IN NOT LOADED WORLD SHOPS, make sure you have backup your shops data, and use /qs cleanghost confirm to continue.");
      return;
    }
    
    if (!"confirm".equalsIgnoreCase(cmdArg[0])) {
      sender.sendMessage(ChatColor.YELLOW
          + "This command will purge all data damaged shop, create in disallow world shop, create disallow sell items shop and IN NOT LOADED WORLD SHOPS, make sure you have backup your shops data, and use /qs cleanghost confirm to continue.");
      return;
    }
    
    sender.sendMessage(ChatColor.GREEN
        + "Starting checking the shop be ghost, all does not exist shop will be removed...");
    
    Bukkit.getScheduler().runTaskAsynchronously(QuickShop.instance(), () -> {
      sender.sendMessage(ChatColor.GREEN + "Async thread is started, please wait...");
      //Util.backupDatabase(); // Already warn the user, don't care about backup result.
      
      QuickShopLoader.instance().forEachShops(shop -> {
        try {
          if (shop.item().getType() == Material.AIR) {
            sender.sendMessage(
                ChatColor.YELLOW + "Shop " + shop + " removing cause item data is damaged.");
            QuickShop.instance().getDatabaseHelper().deleteShop(
                shop.x(), shop.y(), shop.z(), shop.world());
            return;
          }
          
          if (shop.location().world() == null) {
            sender.sendMessage(
                ChatColor.YELLOW + "Shop " + shop + " removing cause target world not loaded.");
            QuickShop.instance().getDatabaseHelper().deleteShop(
                shop.x(), shop.y(), shop.z(), shop.world());
            return;
          }
          
          if (shop.moderators() == null) {
            sender.sendMessage(
                ChatColor.YELLOW + "Shop " + shop + " removing cause owner data is damaged.");
            QuickShop.instance().getDatabaseHelper().deleteShop(
                shop.x(), shop.y(), shop.z(), shop.world());
            return;
          }
          
          // Shop exist check
          QuickShop.instance().getServer().getScheduler().runTask(QuickShop.instance(), () -> {
            Util.debug(
                "Posted to main server thread to continue access Bukkit API for shop " + shop);
            
            if (!Util.canBeShop(shop.location().block())) {
              sender.sendMessage(ChatColor.YELLOW + "Shop " + shop
                  + " removing cause target location nolonger is a shop or disallow create the shop.");
              try {
                QuickShop.instance().getDatabaseHelper().deleteShop(
                    shop.x(), shop.y(), shop.z(), shop.world());
              } catch (SQLException e) {
                return;
              }
            }
          });
        } catch (SQLException e) {
          return;
        }
      });
      
      sender.sendMessage(ChatColor.GREEN + "All shops completed checks.");
    });
  }
}
