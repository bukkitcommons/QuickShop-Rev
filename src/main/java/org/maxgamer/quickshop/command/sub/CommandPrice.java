package org.maxgamer.quickshop.command.sub;

import java.util.Collections;
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
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.shop.ContainerQuickShop;
import org.maxgamer.quickshop.utils.ShopUtils;
import org.maxgamer.quickshop.utils.Util;

import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.stack.Stack;
import cc.bukkit.shop.viewer.ShopViewer;

public class CommandPrice extends QuickShopCommand {
    @Override
    public List<String> permissions() {
        return Collections.singletonList("quickshop.create.changeprice");
    }
    
    @NotNull
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        if (sender instanceof Player)
            return Collections.singletonList(Shop.getLocaleManager().get("tabcomplete.price"));
        
        return Collections.emptyList();
    }
    
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Can't run this command by Console");
            return;
        }
        
        final Player p = (Player) sender;
        
        if (cmdArg.length < 1) {
            sender.sendMessage(Shop.getLocaleManager().get("no-price-given", p));
            return;
        }
        
        final double price;
        final double minPrice = BaseConfig.minimumPrice;
        
        try {
            if (BaseConfig.integerPriceOnly)
                try {
                    price = Long.parseLong(cmdArg[0]);
                } catch (NumberFormatException ex2) {
                    // input is number, but not Integer
                    Util.trace(ex2.getMessage());
                    p.sendMessage(Shop.getLocaleManager().get("not-a-integer", p, cmdArg[0]));
                    return;
                }
            else
                price = Double.parseDouble(cmdArg[0]);
            
        } catch (NumberFormatException ex) {
            // No number input
            Util.trace(ex.getMessage());
            p.sendMessage(Shop.getLocaleManager().get("not-a-number", p, cmdArg[0]));
            return;
        }
        
        final boolean format = BaseConfig.decimalFormatPrice;
        
        if (BaseConfig.allowFreeShops) {
            if (price != 0 && price < minPrice) {
                p.sendMessage(Shop.getLocaleManager().get("price-too-cheap", p, (format) ? ShopUtils.formatPrice(minPrice) : "" + minPrice));
                return;
            }
        } else
            if (price < minPrice) {
                p.sendMessage(Shop.getLocaleManager().get("price-too-cheap", p, (format) ? ShopUtils.formatPrice(minPrice) : "" + minPrice));
                return;
            }
        
        final double price_limit = BaseConfig.maximumPrice;
        
        if (price_limit != -1 && price > price_limit) {
            p.sendMessage(Shop.getLocaleManager().get("price-too-high", p, (format) ? ShopUtils.formatPrice(price_limit) : "" + price_limit));
            return;
        }
        
        double fee = 0;
        
        if (BaseConfig.priceModFee > 0)
            fee = BaseConfig.priceModFee;
        
        /*
         * if (fee > 0 && plugin.getEconomy().getBalance(p.getUniqueId()) < fee) {
         * sender.sendMessage(
         * Shop.getLocaleManager().get("you-cant-afford-to-change-price",
         * plugin.getEconomy().format(fee))); return; }
         */
        final BlockIterator bIt = new BlockIterator(p, 10);
        // Loop through every block they're looking at upto 10 blocks away
        if (!bIt.hasNext()) {
            sender.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop", p));
            return;
        }
        
        while (bIt.hasNext()) {
            final Block b = bIt.next();
            final ShopViewer shop = Shop.getManager().getLoadedShopAt(b.getLocation());
            
            if (shop.isEmpty() || (!shop.get().moderator().isModerator(p.getUniqueId()) && !QuickShopPermissionManager.instance().has(sender, "quickshop.other.price")))
                continue;
            
            if (shop.get().price().equals(price)) {
                // Stop here if there isn't a price change
                sender.sendMessage(Shop.getLocaleManager().get("no-price-change", p));
                return;
            }
            
            if (fee > 0 && !QuickShop.instance().getEconomy().withdraw(p.getUniqueId(), fee)) {
                sender.sendMessage(Shop.getLocaleManager().get("you-cant-afford-to-change-price", p, QuickShop.instance().getEconomy().format(fee)));
                return;
            }
            
            if (fee > 0) {
                sender.sendMessage(Shop.getLocaleManager().get("fee-charged-for-price-change", p, QuickShop.instance().getEconomy().format(fee)));
                try {
                    QuickShop.instance().getEconomy().deposit(QuickShop.instance().getServer().getOfflinePlayer(BaseConfig.taxAccount).getUniqueId(), fee);
                } catch (Exception e) {
                    e.getMessage();
                    ShopLogger.instance().log(Level.WARNING, "QuickShop can't pay tax to the account in config.yml, please set the tax account name to an existing player!");
                }
            }
            // Update the shop
            shop.get().setPrice(Stack.of(price));
            sender.sendMessage(Shop.getLocaleManager().get("price-is-now", p, QuickShop.instance().getEconomy().format(shop.get().<Double>price())));
            // Chest shops can be double shops.
            if (!(shop.get() instanceof ContainerQuickShop))
                return;
            
            final ContainerQuickShop cs = (ContainerQuickShop) shop.get();
            
            if (cs.converse().isEmpty())
                return;
            
            ShopViewer nextTo = cs.getAttachedShop();
            if (nextTo.isEmpty())
                return;
            
            if (cs.is(ShopType.SELLING)) {
                if (cs.price() < nextTo.get().<Double>price())
                    sender.sendMessage(Shop.getLocaleManager().get("buying-more-than-selling", p));
            }
            // Buying
            else
                if (cs.price() > nextTo.get().<Double>price())
                    sender.sendMessage(Shop.getLocaleManager().get("buying-more-than-selling", p));
                
            return;
        }
        
        sender.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop", p));
    }
}
