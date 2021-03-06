package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.shop.QuickShopManager;
import org.maxgamer.quickshop.utils.BlockUtils;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.ShopUtils;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopLocation;
import cc.bukkit.shop.action.data.ShopCreator;

public class CommandCreate extends QuickShopCommand {
  @Override
  public List<String> permissions() {
    return Collections.singletonList("quickshop.create.cmd");
  }

  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    final ArrayList<String> list = new ArrayList<>();

    list.add(Shop.getLocaleManager().get("tabcomplete.price", sender));

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
      sender.sendMessage(Shop.getLocaleManager().get("no-anythings-in-your-hand", sender));
      return;
    }

    final BlockIterator bIt = new BlockIterator((LivingEntity) sender, 10);
    Util.debug("Creating shop");

    while (bIt.hasNext()) {
      final Block b = bIt.next();
      Util.debug("Checking block for shop creation: " + b);

      if (!ShopUtils.canBeShop(b)) {
        Util.debug("Block cannot be shop.");
        continue;
      }

      if (p.isOnline() && !QuickShop.instance().getPermissionChecker().canBuild(p, b)) {
        Util.debug("Failed permission build check, canceled");
        return;
      }

      if (!QuickShopManager.canBuildShop(p, b)) {
        // As of the new checking system, most plugins will tell the
        // player why they can't create a shop there.
        // So telling them a message would cause spam etc.
        Util.debug("Util report you can't build shop there.");
        return;
      }

      if (BlockUtils.getSecondHalf(b).isPresent()
          && !QuickShopPermissionManager.instance().has(p, "quickshop.create.double")) {
        p.sendMessage(Shop.getLocaleManager().get("no-double-chests", sender));
        return;
      }

      if (Util.isBlacklisted(item) && !QuickShopPermissionManager.instance().has(p,
          "quickshop.bypass." + item.getType().name())) {
        p.sendMessage(Shop.getLocaleManager().get("blacklisted-item", sender));
        return;
      }

      // Send creation menu.
      Shop.getActions().setAction(p.getUniqueId(),
          ShopCreator.create(
              ShopLocation.of(b.getLocation()),
              b.getRelative(BlockUtils.yawToFace(p.getLocation().getYaw())), item));

      if (cmdArg.length >= 1) {
        Shop.getActions().handleChat(p, cmdArg[0], false);
        Util.debug("Created by handle chat");
        return;
      }

      p.sendMessage(
          Shop.getLocaleManager().get("how-much-to-trade-for", sender, ItemUtils.getItemStackName(item)));
      Util.debug("Created by wait chat");
      return;
    }
  }
}
