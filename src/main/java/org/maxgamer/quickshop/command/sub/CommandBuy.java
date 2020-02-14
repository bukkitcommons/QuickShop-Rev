package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.ItemUtils;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.viewer.BlockViewer;
import cc.bukkit.shop.viewer.ViewAction;

public class CommandBuy extends QuickShopCommand {
  @Override
  public void onCommand(
      @NotNull CommandSender sender,
      @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    
    if (!(sender instanceof Player)) {
      sender.sendMessage(Shop.getLocaleManager().get("Can't run command by Console", sender));
      return;
    }

    Runnable notLookingAtShop = () -> sender.sendMessage(
        Shop.getLocaleManager().get("not-looking-at-shop", sender));

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
                    Shop.getLocaleManager().get("command.now-buying", sender, ItemUtils.getItemStackName(shop.getItem())));

                return ViewAction.BREAK;
              }, ViewAction.NEXT);

        }).ifNone(notLookingAtShop);
  }
  
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.create.buy");
  }
}
