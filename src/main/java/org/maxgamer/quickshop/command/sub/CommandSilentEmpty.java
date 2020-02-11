package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.shop.ContainerQuickShop;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandSilentEmpty extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.empty");
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (cmdArg.length < 4) {
      Util.debug("Exception on command, cancel.");
      return;
    }

    final ShopViewer shop =
        Shop.getManager().getLoadedShopAt(new Location(Bukkit.getWorld(cmdArg[0]),
            Integer.parseInt(cmdArg[1]), Integer.parseInt(cmdArg[2]), Integer.parseInt(cmdArg[3])));

    if (!(shop.get() instanceof ContainerQuickShop)) {
      sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("not-looking-at-shop", sender));
      return;
    }

    final ContainerQuickShop cs = (ContainerQuickShop) shop.get();
    final Inventory inventory = cs.getInventory();

    if (inventory == null) {
      // TODO: 24/11/2019 Send message about that issue.
      return;
    }

    inventory.clear();
    QuickShop.instance().getLocaleManager().sendControlPanelInfo(sender, shop.get());
    sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("empty-success", sender));
  }
}
