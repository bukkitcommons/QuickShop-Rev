package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.shop.ContainerQuickShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandEmpty extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.empty");
  }
  
  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Can't run this command from Console");
      return;
    }

    final BlockIterator bIt = new BlockIterator((LivingEntity) sender, 10);

    if (!bIt.hasNext()) {
      sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("not-looking-at-shop", sender));
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
        sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("empty-success", sender));
      } else {
        sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("not-looking-at-shop", sender));
      }

      return;
    }

    sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("not-looking-at-shop", sender));
  }
}
