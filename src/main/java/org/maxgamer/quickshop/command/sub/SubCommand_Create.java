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
import org.maxgamer.quickshop.shop.ShopActionManager;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.data.ShopAction;
import org.maxgamer.quickshop.shop.api.data.ShopCreator;
import org.maxgamer.quickshop.shop.api.data.ShopLocation;
import org.maxgamer.quickshop.shop.api.data.ShopSnapshot;
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
    Util.debug("Creating shop");

    while (bIt.hasNext()) {
      final Block b = bIt.next();
      Util.debug("Checking block for shop creation: " + b);

      if (!Util.canBeShop(b)) {
        Util.debug("Block cannot be shop.");
        continue;
      }

      if (p.isOnline() && !QuickShop.instance().getPermissionChecker().canBuild(p, b)) {
        Util.debug("Failed permission build check, canceled");
        return;
      }

      if (!ShopManager.canBuildShop(p, b)) {
        // As of the new checking system, most plugins will tell the
        // player why they can't create a shop there.
        // So telling them a message would cause spam etc.
        Util.debug("Util report you can't build shop there.");
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
      ShopActionManager.instance().setAction(p.getUniqueId(),
          ShopCreator.create(ShopLocation.of(b.getLocation()), b.getRelative(p.getFacing().getOppositeFace()), item));

      if (cmdArg.length >= 1) {
        ShopActionManager.instance().handleChat(p, cmdArg[0], false);
        Util.debug("Created by handle chat");
        return;
      }

      p.sendMessage(
          MsgUtil.getMessage("how-much-to-trade-for", sender, Util.getItemStackName(item)));
      Util.debug("Created by wait chat");
      return;
    }
  }
}
