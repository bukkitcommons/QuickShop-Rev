package org.maxgamer.quickshop.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.command.SneakyTabs;
import cc.bukkit.shop.viewer.BlockViewer;
import cc.bukkit.shop.viewer.ViewAction;

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
        .get((Player) sender, 10)
        .ifEmpty(notLookingAtShop)
        
        .forEach(block -> {

          return Shop.getManager()
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
