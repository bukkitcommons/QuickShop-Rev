package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.shop.QuickShopManager;
import org.maxgamer.quickshop.shop.QuickShopActionManager;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.data.ShopCreator;
import cc.bukkit.shop.data.ShopLocation;

public class CommandSuperCreate extends QuickShopCommand {

  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    final ArrayList<String> list = new ArrayList<>();

    list.add(MsgUtil.getMessage("tabcomplete.amount", sender));

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

    while (bIt.hasNext()) {
      final Block b = bIt.next();

      if (!Util.canBeShop(b)) {
        Util.debug("Block cannot be shop.");
        continue;
      }

      if (!QuickShopManager.canBuildShop(p, b)) {
        // As of the new checking system, most plugins will tell the
        // player why they can't create a shop there.
        // So telling them a message would cause spam etc.
        Util.debug("Util report you can't build shop there.");
        return;
      }

      if (Util.getSecondHalf(b).isPresent()
          && !PermissionManager.instance().has(sender, "quickshop.create.double")) {
        p.sendMessage(MsgUtil.getMessage("no-double-chests", sender));
        return;
      }

      if (Util.isBlacklisted(item) && !PermissionManager.instance().has(p,
          "quickshop.bypass." + item.getType().name())) {
        p.sendMessage(MsgUtil.getMessage("blacklisted-item", sender));
        return;
      }

      if (cmdArg.length >= 1) {
        Shop.getActions().handleChat(p, cmdArg[0], true);
        return;
      }
      // Send creation menu.
      final ShopCreator info = ShopCreator.create(ShopLocation.of(b.getLocation()), b.getRelative(p.getFacing().getOppositeFace()), item);

      QuickShopActionManager.instance().getActions().put(p.getUniqueId(), info);
      p.sendMessage(
          MsgUtil.getMessage("how-much-to-trade-for", sender, Util.getItemStackName(item)));
      return;
    }
    sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
  }
}