package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandRefill extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.refill");
  }

  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    final ArrayList<String> list = new ArrayList<>();

    list.add(Shop.getLocaleManager().get("tabcomplete.amount"));

    return list;
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Can't run by Console");
      return;
    }

    if (cmdArg.length < 1) {
      sender.sendMessage(Shop.getLocaleManager().get("command.no-amount-given"));
      return;
    }

    final int add;

    try {
      add = Integer.parseInt(cmdArg[0]);
    } catch (NumberFormatException e) {
      sender.sendMessage(Shop.getLocaleManager().get("thats-not-a-number"));
      return;
    }

    final BlockIterator bIt = new BlockIterator((LivingEntity) sender, 10);

    if (!bIt.hasNext()) {
      sender.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop"));
      return;
    }

    while (bIt.hasNext()) {
      final Block b = bIt.next();
      final ShopViewer shop = Shop.getManager().getLoadedShopAt(b.getLocation());

      if (!shop.isPresent()) {
        continue;
      }

      shop.<ChestShop>get().fill(add);
      sender.sendMessage(Shop.getLocaleManager().get("refill-success"));
      return;
    }

    sender.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop"));
  }
}
