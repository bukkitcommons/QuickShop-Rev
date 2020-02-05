package org.maxgamer.quickshop.command.sub;

import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.command.SneakyTabs;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.viewer.BlockViewer;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;
import org.maxgamer.quickshop.utils.viewer.ViewAction;

public class SubCommand_Remove extends SneakyTabs implements CommandProcesser {

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
        ShopViewer shop = ShopManager.instance().getShopAt(block);
        
        if (shop.isPresent()) {
          if (shop.get().getModerator().isModerator(((Player) sender).getUniqueId())
              || QuickShop.getPermissionManager().hasPermission(sender, "quickshop.other.destroy")) {
            shop.get().onUnload();
            shop.get().delete();
          } else {
            sender.sendMessage(ChatColor.RED + MsgUtil.getMessage("no-permission", sender));
          }
        }
        
        return ViewAction.NEXT;
      })
      
      .ifNone(notLookingAtShop);
  }
}
