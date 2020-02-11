package org.maxgamer.quickshop.command.sub;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.shop.ContainerQuickShop;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.command.CommandProcesser;

import cc.bukkit.shop.viewer.ShopViewer;

public class CommandEmpty extends QuickShopCommand {
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Can't run this command from Console");
      return;
    }

    final BlockIterator bIt = new BlockIterator((LivingEntity) sender, 10);

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

      if (shop.get() instanceof ContainerQuickShop) {
        final ContainerQuickShop cs = (ContainerQuickShop) shop.get();
        final Inventory inventory = cs.getInventory();

        if (inventory == null) {
          // TODO: 24/11/2019 Send message about that issue.
          return;
        }

        cs.getInventory().clear();
        sender.sendMessage(MsgUtil.getMessage("empty-success", sender));
      } else {
        sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
      }

      return;
    }

    sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
  }
}
