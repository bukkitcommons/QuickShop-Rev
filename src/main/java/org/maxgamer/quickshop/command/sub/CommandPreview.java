package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.shop.ItemPreviewer;
import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.BlockViewer;
import cc.bukkit.shop.viewer.ShopViewer;
import cc.bukkit.shop.viewer.ViewAction;

public class CommandPreview extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.preview");
  }
  
  @Override
  public boolean hidden() {
    return true;
  }
  
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      
      if (args.length == 4) { // With world param
        handleAt(player, args[0], args[1], args[2], args[3]);
        return;
      }
      
      if (args.length == 3) { // With pos param
        handleAt(player, player.getWorld().getName(), args);
        return;
      }

      BlockViewer viwer = BlockViewer.get(((Player) sender), 10);
      viwer
        .forEach(block -> {
          ShopViewer shopViewer = Shop.getManager().getLoadedShopAt(block);

          if (shopViewer.isPresent()) {
            handleShop(player, shopViewer.get());
            return ViewAction.BREAK;
          }
          
          return ViewAction.NEXT;
        })
        .ifNone(() -> sender.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop")));
    }
    
    sender.sendMessage("Can't run this command from Console");
    return;
  }
  
  private final static void handleAt(@NotNull Player player, @NotNull String world, @NotNull String... pos) {
    try {
      ShopViewer viewer = Shop.getManager().getLoadedShopAt(world,
          Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), Integer.parseInt(pos[2]));
      
      if (viewer.isEmpty()) {
        // FIXME not exist
        player.sendMessage(Shop.getLocaleManager().get("shop-not-exist", player));
        return;
      }
      
      handleShop(player, viewer.get());
    } catch (NumberFormatException e) {
      player.sendMessage(Shop.getLocaleManager().get("not-a-integer", player));
    }
  }
  
  private final static void handleShop(@NotNull Player player, @NotNull ChestShop shop) {
    new ItemPreviewer(shop.stack(), player).show();
  }
}
