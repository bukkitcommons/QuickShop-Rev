package org.maxgamer.quickshop.command.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.shop.QuickShopLoader;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.command.CommandProcesser;

import cc.bukkit.shop.viewer.BlockViewer;
import cc.bukkit.shop.viewer.ShopViewer;
import cc.bukkit.shop.viewer.ViewAction;

public class CommandRemove extends QuickShopCommand {

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(ChatColor.RED + "Only players may use that command.");
      return;
    }
    
    BlockViewer viwer = BlockViewer.get(((Player) sender), 10);
    Runnable notLookingAtShop = () -> sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
    
    viwer
      .ifEmpty(notLookingAtShop)
      
      .forEach(block -> {
        ShopViewer shop = Shop.getManager().getLoadedShopAt(block);
        
        if (shop.isPresent()) {
          if (shop.get().getModerator().isModerator(((Player) sender).getUniqueId())
              || PermissionManager.instance().has(sender, "quickshop.other.destroy")) {
            Shop.getLoader().delete(shop.get());
          } else {
            sender.sendMessage(ChatColor.RED + MsgUtil.getMessage("no-permission", sender));
          }
          
          return ViewAction.BREAK;
        }
        
        return ViewAction.NEXT;
      })
      
      .ifNone(notLookingAtShop);
  }
}