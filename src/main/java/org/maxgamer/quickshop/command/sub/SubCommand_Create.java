package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.BitField;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.CommandProcesser;
import org.maxgamer.quickshop.shop.data.ShopAction;
import org.maxgamer.quickshop.shop.data.ShopCreator;
import org.maxgamer.quickshop.shop.data.ShopSnapshot;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;

public class SubCommand_Create implements CommandProcesser {
  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    final ArrayList<String> list = new ArrayList<>();

    list.add(MsgUtil.getMessage("tabcomplete.price", sender));

    return list;
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("This command can't be run by console");
      return;
    }

    final Player p = (Player) sender;
    final ItemStack item = p.getInventory().getItemInMainHand();

    if (item.getType() == Material.AIR) {
      sender.sendMessage(MsgUtil.getMessage("no-anythings-in-your-hand", sender));
      return;
    }

    final BlockIterator bIt = new BlockIterator((LivingEntity) sender, 10);
    Util.debugLog("Creating shop");

    while (bIt.hasNext()) {
      final Block b = bIt.next();
      Util.debugLog("Checking block for shop creation: " + b);

      if (!Util.canBeShop(b)) {
        continue;
      }

      if (p.isOnline() && !QuickShop.instance().getPermissionChecker().canBuild(p, b)) {
        Util.debugLog("Failed permission build check, canceled");
        return;
      }

      BlockFace blockFace;
      try {
        blockFace = p.getFacing();
      } catch (Throwable throwable) {
        blockFace = Util.getYawFace(p.getLocation().getYaw());
      }

      if (!QuickShop.instance().getShopManager().canBuildShop(p, b, blockFace)) {
        // As of the new checking system, most plugins will tell the
        // player why they can't create a shop there.
        // So telling them a message would cause spam etc.
        Util.debugLog("Util report you can't build shop there.");
        return;
      }

      if (Util.getSecondHalf(b) != null
          && !QuickShop.getPermissionManager().hasPermission(p, "quickshop.create.double")) {
        p.sendMessage(MsgUtil.getMessage("no-double-chests", sender));
        return;
      }

      if (Util.isBlacklisted(item) && !QuickShop.getPermissionManager().hasPermission(p,
          "quickshop.bypass." + item.getType().name())) {
        p.sendMessage(MsgUtil.getMessage("blacklisted-item", sender));
        return;
      }

      // Send creation menu.
      QuickShop.instance().getShopManager().getActions().put(p.getUniqueId(),
          new ShopCreator(b.getLocation(), item, b.getRelative(p.getFacing().getOppositeFace())));

      if (cmdArg.length >= 1) {
        QuickShop.instance().getShopManager().handleChat(p, cmdArg[0]);
        Util.debugLog("Created by handle chat");
        return;
      }

      p.sendMessage(
          MsgUtil.getMessage("how-much-to-trade-for", sender, Util.getItemStackName(item)));
      Util.debugLog("Created by wait chat");
      return;
    }
  }
}