package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandStaff extends QuickShopCommand {
  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    final ArrayList<String> tabList = new ArrayList<>();

    Util.debug(Util.array2String(cmdArg));

    if (cmdArg.length < 2) {
      if (cmdArg.length == 1) {
        final String prefix = cmdArg[0].toLowerCase();

        if ("add".startsWith(prefix) || "add".equals(prefix)) {
          tabList.add("add");
        }

        if ("del".startsWith(prefix) || "del".equals(prefix)) {
          tabList.add("del");
        }

        if ("list".startsWith(prefix) || "list".equals(prefix)) {
          tabList.add("list");
        }

        if ("clear".startsWith(prefix) || "clear".equals(prefix)) {
          tabList.add("clear");
        }
      } else {
        tabList.add("add");
        tabList.add("del");
        tabList.add("list");
        tabList.add("clear");
      }

      return tabList;
    }

    return tabList;
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    Util.debug(Util.array2String(cmdArg));
    if (!(sender instanceof Player)) {
      sender.sendMessage("Only player can execute this command.");
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

      if (!shop.isPresent() || !shop.get().getModerator().isModerator(((Player) sender).getUniqueId())) {
        continue;
      }

      switch (cmdArg.length) {
        case 0:
          sender.sendMessage(MsgUtil.getMessage("command.wrong-args", sender));
          return;
        case 1:
          switch (cmdArg[0]) {
            case "add":
              sender.sendMessage(MsgUtil.getMessage("command.wrong-args", sender));
              return;
            case "del":
              sender.sendMessage(MsgUtil.getMessage("command.wrong-args", sender));
              return;
            case "clear":
              shop.get().clearStaffs();
              sender.sendMessage(MsgUtil.getMessage("shop-staff-cleared", sender));
              return;
            case "list":
              final Set<UUID> staffs = shop.get().getStaffs();

              if (staffs.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN
                    + MsgUtil.getMessage("tableformat.left_begin", sender) + "Empty");
                return;
              }

              for (UUID uuid : staffs) {
                sender.sendMessage(
                    ChatColor.GREEN + MsgUtil.getMessage("tableformat.left_begin", sender)
                        + Bukkit.getOfflinePlayer(uuid).getName());
              }

              return;
            default:
              sender.sendMessage(MsgUtil.getMessage("command.wrong-args", sender));
          }

          break;
        case 2:
          final OfflinePlayer offlinePlayer = QuickShop.instance().getServer().getOfflinePlayer(cmdArg[1]);
          String offlinePlayerName = offlinePlayer.getName();

          if (offlinePlayerName == null) {
            offlinePlayerName = "null";
          }

          switch (cmdArg[0]) {
            case "add":
              shop.get().addStaff(offlinePlayer.getUniqueId());
              sender.sendMessage(MsgUtil.getMessage("shop-staff-added", sender, offlinePlayerName));
              return;
            case "del":
              sender
                  .sendMessage(MsgUtil.getMessage("shop-staff-deleted", sender, offlinePlayerName));
              return;
            default:
              sender.sendMessage(MsgUtil.getMessage("command.wrong-args", sender));
          }

          break;
        default:
          Util.debug("No any args matched");
          break;
      }
    }
  }
}
