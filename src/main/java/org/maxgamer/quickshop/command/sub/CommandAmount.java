package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import cc.bukkit.shop.Shop;

public class CommandAmount extends QuickShopCommand {

  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    final ArrayList<String> list = new ArrayList<>();

    list.add(QuickShop.instance().getLocaleManager().getMessage("tabcomplete.amount", sender));

    return list;
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (cmdArg.length < 1) {
      sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("command.wrong-args", sender));
      return;
    }

    if (!(sender instanceof Player)) {
      sender.sendMessage("This command can't be run by console");
      return;
    }

    final Player player = (Player) sender;

    if (!Shop.getActions().hasAction(player.getUniqueId())) {
      sender.sendMessage(QuickShop.instance().getLocaleManager().getMessage("no-pending-action", sender));
      return;
    }

    Shop.getActions().handleChat(player, cmdArg[0], false);
  }
}
