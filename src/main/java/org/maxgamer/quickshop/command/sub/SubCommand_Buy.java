package org.maxgamer.quickshop.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.command.SneakyTabs;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.ShopType;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.viewer.BlockViewer;
import org.maxgamer.quickshop.utils.viewer.ViewAction;

public class SubCommand_Buy extends SneakyTabs implements CommandProcesser {
  @Override
  public void onCommand(
      @NotNull CommandSender sender,
      @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    
    if (!(sender instanceof Player)) {
      sender.sendMessage(MsgUtil.getMessage("Can't run command by Console", sender));
      return;
    }

    Runnable notLookingAtShop = () -> sender.sendMessage(
        MsgUtil.getMessage("not-looking-at-shop", sender));

    BlockViewer
        .get((Entity) sender, 10)
        .ifEmpty(notLookingAtShop)
        
        .forEach(block -> {

          return ShopManager
              .instance()
              .getLoadedShopAt(block)
              
              .nonNull()
              .filter(shop -> shop.getModerator()
                                  .isModerator(((Player) sender)
                                  .getUniqueId()))
              
              .apply(shop -> {
                shop.setShopType(ShopType.BUYING);
                shop.setSignText();
                shop.save();
                sender.sendMessage(
                    MsgUtil.getMessage("command.now-buying", sender, Util.getItemStackName(shop.getItem())));

                return ViewAction.BREAK;
              }, ViewAction.NEXT);

        }).ifNone(notLookingAtShop);
  }
}
