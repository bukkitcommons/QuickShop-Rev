package org.maxgamer.quickshop.command.sub;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.shop.ContainerQuickShop;
import org.maxgamer.quickshop.shop.QuickShopLoader;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.command.SneakyTabs;
import cc.bukkit.shop.data.ShopData;

public class SubCommand_Info extends SneakyTabs implements CommandProcesser {

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    int buying, selling, doubles, chunks, worlds, doubleschests;
    buying = selling = doubles = chunks = worlds = doubleschests = 0;
    int nostock = 0;
    ContainerShop shop;

    for (Map<Long, Map<Long, ShopData>> inWorld : QuickShopLoader.instance().getShopsMap().values()) {
      worlds++;

      for (Map<Long, ShopData> inChunk : inWorld.values()) {
        chunks++;
        // noinspection unchecked
        for (ShopData data : inChunk.values()) {
          if (data.type() == ShopType.BUYING) {
            buying++;
          } else {
            selling++;
          }
          
          try {
            shop = Shop.getManager().load(Bukkit.getWorld(data.world()), data);
          } catch (InvalidConfigurationException e) {
            continue;
          }

          if (shop instanceof ContainerQuickShop && ((ContainerQuickShop) shop).isDualShop()) {
            doubles++;
          } else if (data.type() == ShopType.SELLING && shop.getRemainingStock() == 0) {
            nostock++;
          }

          if (shop instanceof ContainerQuickShop && ((ContainerQuickShop) shop).isDoubleChestShop()) {
            doubleschests++;
          }
        }
      }
    }

    sender.sendMessage(ChatColor.RED + "QuickShop Statistics...");
    sender.sendMessage(ChatColor.GREEN + "Server UniqueID: " + BaseConfig.serverUUID);
    sender.sendMessage(ChatColor.GREEN + "" + (buying + selling) + " shops in " + chunks
        + " chunks spread over " + worlds + " worlds.");
    sender.sendMessage(ChatColor.GREEN + "" + doubles + " double shops. (" + doubleschests
        + " shops create on double chest.)");
    sender.sendMessage(ChatColor.GREEN + "" + nostock
        + " nostock selling shops (excluding doubles) which will be removed by /qs clean.");
    sender.sendMessage(ChatColor.GREEN + "QuickShop " + QuickShop.instance().getVersion());
  }
}
