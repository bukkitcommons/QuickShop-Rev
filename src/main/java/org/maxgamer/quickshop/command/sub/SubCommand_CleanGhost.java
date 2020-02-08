package org.maxgamer.quickshop.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.command.SneakyTabs;
import org.maxgamer.quickshop.shop.ShopLoader;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.utils.Util;

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
      
      for (Shop shop : ShopLoader.instance().getAllShop()) {
        if (shop.getItem().getType() == Material.AIR) {
          sender.sendMessage(
              ChatColor.YELLOW + "Shop " + shop + " removing cause item data is damaged.");
          ShopLoader.instance().delete(shop);
          continue;
        }
        
        if (shop.getLocation().getWorld() == null) {
          sender.sendMessage(
              ChatColor.YELLOW + "Shop " + shop + " removing cause target world not loaded.");
          ShopLoader.instance().delete(shop);
          continue;
        }
        
        if (shop.getOwner() == null) {
          sender.sendMessage(
              ChatColor.YELLOW + "Shop " + shop + " removing cause owner data is damaged.");
          ShopLoader.instance().delete(shop);
          continue;
        }
        
        // Shop exist check
        QuickShop.instance().getServer().getScheduler().runTask(QuickShop.instance(), () -> {
          Util.debug(
              "Posted to main server thread to continue access Bukkit API for shop " + shop);
          
          if (!Util.canBeShop(shop.getLocation().getBlock())) {
            sender.sendMessage(ChatColor.YELLOW + "Shop " + shop
                + " removing cause target location nolonger is a shop or disallow create the shop.");
            ShopLoader.instance().delete(shop);
          }
        }); // Post to server main thread to check.
        
        sender.sendMessage(ChatColor.GREEN + "All shops completed checks.");
      }
    });
  }
}
