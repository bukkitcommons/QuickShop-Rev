package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.messages.ShopMessager;
import cc.bukkit.shop.command.CommandProcesser;


public class CommandFetchMessage  extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.fetchmessage");
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(ChatColor.RED + "Only players may use that command.");
      return;
    }

    QuickShop.instance()
             .getServer()
             .getScheduler()
             .runTask(QuickShop.instance(), () -> ShopMessager.flushMessagesFor((Player) sender));
  }
}
