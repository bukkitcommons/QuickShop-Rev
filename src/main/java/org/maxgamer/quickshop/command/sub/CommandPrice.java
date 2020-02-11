package org.maxgamer.quickshop.command.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.shop.ContainerQuickShop;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.command.CommandProcesser;
import cc.bukkit.shop.util.ShopLogger;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandPrice extends QuickShopCommand {

  @NotNull
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    ArrayList<String> list = new ArrayList<>();
    list.add(MsgUtil.getMessage("tabcomplete.price", sender));
    return list;
  }

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Can't run this command by Console");
      return;
    }

    final Player p = (Player) sender;

    if (cmdArg.length < 1) {
      sender.sendMessage(MsgUtil.getMessage("no-price-given", sender));
      return;
    }

    final double price;
    final double minPrice = BaseConfig.minimumPrice;

    try {
      if (BaseConfig.integerPriceOnly) {
        try {
          price = Long.parseLong(cmdArg[0]);
        } catch (NumberFormatException ex2) {
          // input is number, but not Integer
          Util.debug(ex2.getMessage());
          p.sendMessage(MsgUtil.getMessage("not-a-integer", p, cmdArg[0]));
          return;
        }
      } else {
        price = Double.parseDouble(cmdArg[0]);
      }

    } catch (NumberFormatException ex) {
      // No number input
      Util.debug(ex.getMessage());
      p.sendMessage(MsgUtil.getMessage("not-a-number", p, cmdArg[0]));
      return;
    }

    final boolean format = BaseConfig.decimalFormatPrice;

    if (BaseConfig.allowFreeShops) {
      if (price != 0 && price < minPrice) {
        p.sendMessage(MsgUtil.getMessage("price-too-cheap", p,
            (format) ? MsgUtil.decimalFormat(minPrice) : "" + minPrice));
        return;
      }
    } else {
      if (price < minPrice) {
        p.sendMessage(MsgUtil.getMessage("price-too-cheap", p,
            (format) ? MsgUtil.decimalFormat(minPrice) : "" + minPrice));
        return;
      }
    }

    final double price_limit = BaseConfig.maximumPrice;

    if (price_limit != -1 && price > price_limit) {
      p.sendMessage(MsgUtil.getMessage("price-too-high", p,
          (format) ? MsgUtil.decimalFormat(price_limit) : "" + price_limit));
      return;
    }

    double fee = 0;

    if (QuickShop.instance().isPriceChangeRequiresFee()) {
      fee = BaseConfig.priceModFee;
    }

    /*
     * if (fee > 0 && plugin.getEconomy().getBalance(p.getUniqueId()) < fee) { sender.sendMessage(
     * MsgUtil.getMessage("you-cant-afford-to-change-price", plugin.getEconomy().format(fee)));
     * return; }
     */
    final BlockIterator bIt = new BlockIterator(p, 10);
    // Loop through every block they're looking at upto 10 blocks away
    if (!bIt.hasNext()) {
      sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
      return;
    }

    while (bIt.hasNext()) {
      final Block b = bIt.next();
      final ShopViewer shop = Shop.getManager().getLoadedShopAt(b.getLocation());

      if (shop.isEmpty() || (!shop.get().getModerator().isModerator(((Player) sender).getUniqueId())
          && !PermissionManager.instance().has(sender, "quickshop.other.price"))) {
        continue;
      }
      
      if (shop.get().getPrice() == price) {
        // Stop here if there isn't a price change
        sender.sendMessage(MsgUtil.getMessage("no-price-change", sender));
        return;
      }

      if (fee > 0 && !QuickShop.instance().getEconomy().withdraw(p.getUniqueId(), fee)) {
        sender.sendMessage(MsgUtil.getMessage("you-cant-afford-to-change-price", sender,
            QuickShop.instance().getEconomy().format(fee)));
        return;
      }

      if (fee > 0) {
        sender.sendMessage(MsgUtil.getMessage("fee-charged-for-price-change", sender,
            QuickShop.instance().getEconomy().format(fee)));
        try {
          QuickShop.instance().getEconomy().deposit(
              QuickShop.instance().getServer().getOfflinePlayer(BaseConfig.taxAccount).getUniqueId(), fee);
        } catch (Exception e) {
          e.getMessage();
          ShopLogger.instance().log(Level.WARNING,
              "QuickShop can't pay tax to the account in config.yml, please set the tax account name to an existing player!");
        }
      }
      // Update the shop
      shop.get().setPrice(price);
      sender.sendMessage(
          MsgUtil.getMessage("price-is-now", sender, QuickShop.instance().getEconomy().format(shop.get().getPrice())));
      // Chest shops can be double shops.
      if (!(shop.get() instanceof ContainerQuickShop)) {
        return;
      }

      final ContainerQuickShop cs = (ContainerQuickShop) shop.get();

      if (!cs.isDualShop()) {
        return;
      }

      final ContainerShop nextTo = cs.getAttachedShop();

      if (nextTo == null) {
        // TODO: 24/11/2019 Send message about that issue.
        return;
      }

      if (cs.is(ShopType.SELLING)) {
        if (cs.getPrice() < nextTo.getPrice()) {
          sender.sendMessage(MsgUtil.getMessage("buying-more-than-selling", sender));
        }
      }
      // Buying
      else if (cs.getPrice() > nextTo.getPrice()) {
        sender.sendMessage(MsgUtil.getMessage("buying-more-than-selling", sender));
      }

      return;
    }

    sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop", sender));
  }
}