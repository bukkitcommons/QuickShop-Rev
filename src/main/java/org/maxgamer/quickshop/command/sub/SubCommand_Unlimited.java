package org.maxgamer.quickshop.command.sub;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.command.SneakyTabs;
import cc.bukkit.shop.viewer.ShopViewer;

public class SubCommand_Unlimited extends SneakyTabs implements CommandProcesser {
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Only player can run this command.");
      return;
    }

    final BlockIterator bIt = new BlockIterator((Player) sender, 10);

    if (!bIt.hasNext()) {
      sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
      return;
    }

    while (bIt.hasNext()) {
      final Block b = bIt.next();
      final ShopViewer shop = Shop.getManager().getLoadedShopAt(b.getLocation());

      if (!shop.isPresent()) {
        continue;
      }

      shop.get().setUnlimited(!shop.get().isUnlimited());

      if (shop.get().isUnlimited()) {
        sender.sendMessage(MsgUtil.getMessage("command.toggle-unlimited.unlimited", sender));
        return;
      }

      sender.sendMessage(MsgUtil.getMessage("command.toggle-unlimited.limited", sender));

      return;
    }

    sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
  }
}
