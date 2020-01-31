package org.maxgamer.quickshop.command.sub;

import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.command.SneakyTabs;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.utils.MsgUtil;

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
      final Optional<Shop> shop = QuickShop.instance().getShopManager().getShop(b.getLocation());

      if (!shop.isPresent()) {
        continue;
      }

      shop.get().setUnlimited(!shop.get().isUnlimited());
      // shop.setSignText();
      shop.get().update();

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
