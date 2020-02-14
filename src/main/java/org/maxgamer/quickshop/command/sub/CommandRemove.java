package org.maxgamer.quickshop.command.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.utils.ItemUtils;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.BlockViewer;
import cc.bukkit.shop.viewer.ShopViewer;
import cc.bukkit.shop.viewer.ViewAction;

public class CommandRemove extends QuickShopCommand {

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
    if (args.length == 4) { // With world param, can be executed by console
      handleAt(sender, args[0], args[1], args[2], args[3]);
      return;
    }
    
    if (sender instanceof Player) {
      Player player = (Player) sender;
      
      if (args.length == 3) { // With pos param
        handleAt(sender, player.getWorld().getName(), args);
        return;
      }
      
      BlockViewer viwer = BlockViewer.get(((Player) sender), 10);
      viwer
        .forEach(block -> {
          ShopViewer shopViewer = Shop.getManager().getLoadedShopAt(block);

          if (shopViewer.isPresent()) {
            handleShop(sender, shopViewer.get());
            return ViewAction.BREAK;
          }
          
          return ViewAction.NEXT;
        })
        .ifNone(() -> sender.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop", sender)));
      
      return;
    }
    
    sender.sendMessage(ChatColor.RED + "Only players may use that command.");
    return;
  }
  
  private final static void handleAt(@NotNull CommandSender sender, @NotNull String world, @NotNull String... pos) {
    try {
      ShopViewer viewer = Shop.getManager().getLoadedShopAt(world,
          Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), Integer.parseInt(pos[2]));
      
      if (viewer.isEmpty()) {
        // FIXME not exist
        sender.sendMessage(Shop.getLocaleManager().get("shop-not-exist", sender));
        return;
      }
      
      handleShop(sender, viewer.get());
    } catch (NumberFormatException e) {
      sender.sendMessage(Shop.getLocaleManager().get("not-a-integer", sender));
    }
  }
  
  private final static void handleShop(@NotNull CommandSender sender, @NotNull ContainerShop shop) {
    if (!QuickShopPermissionManager.instance().has(sender, "quickshop.other.destroy") &&
        !shop.isModerator(((Player) sender).getUniqueId())) {
      sender.sendMessage(Shop.getLocaleManager().get("no-permission", sender));
      return;
    }
    
    Shop.getLoader().delete(shop);
    
    sender.sendMessage(
        Shop.getLocaleManager().get(
            "command.now-selling", sender, ItemUtils.getItemStackName(shop.getItem())));
  }
}
