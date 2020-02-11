package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandSell extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.create.sell");
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(Shop.getLocaleManager().getMessage("Can't run command by Console", sender));
      return;
    }

    final BlockIterator bIt = new BlockIterator((LivingEntity) sender, 10);

    if (!bIt.hasNext()) {
      sender.sendMessage(Shop.getLocaleManager().getMessage("not-looking-at-shop", sender));
      return;
    }

    while (bIt.hasNext()) {
      final Block b = bIt.next();
      final ShopViewer shop = Shop.getManager().getLoadedShopAt(b.getLocation());

      if (!shop.isPresent() || !shop.get().getModerator().isModerator(((Player) sender).getUniqueId())) {
        continue;
      }

      shop.get().setShopType(ShopType.SELLING);
      shop.get().setSignText();
      shop.get().save();
      sender.sendMessage(
          Shop.getLocaleManager().getMessage("command.now-selling", sender, Util.getItemStackName(shop.get().getItem())));
      return;
    }
    sender.sendMessage(Shop.getLocaleManager().getMessage("not-looking-at-shop", sender));
  }
}
