package org.maxgamer.quickshop.shop;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.QuickShopLocaleManager;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.utils.BlockUtils;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.JavaUtils;
import org.maxgamer.quickshop.utils.ShopUtils;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Maps;
import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.action.ShopAction;
import cc.bukkit.shop.action.ShopActionData;
import cc.bukkit.shop.action.ShopCreator;
import cc.bukkit.shop.action.ShopSnapshot;
import cc.bukkit.shop.buyer.BuyerShop;
import cc.bukkit.shop.event.ShopCreateEvent;
import cc.bukkit.shop.event.ShopPurchaseEvent;
import cc.bukkit.shop.event.ShopSuccessPurchaseEvent;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.manager.ShopActionManager;
import cc.bukkit.shop.moderator.ShopModerator;
import cc.bukkit.shop.seller.SellerShop;
import cc.bukkit.shop.viewer.ShopViewer;

public class QuickShopActionManager implements ShopActionManager {
  /*
   * Singleton
   */
  private static class LazySingleton {
    private static final QuickShopActionManager INSTANCE = new QuickShopActionManager();
  }
  
  private QuickShopActionManager() {}
  
  public static QuickShopActionManager instance() {
    return LazySingleton.INSTANCE;
  }
  
  /*
   * Action manager
   */
  private Map<UUID, ShopActionData> actionData = Maps.newConcurrentMap();
  
  @Override
  public boolean hasAction(@NotNull UUID player) {
    return actionData.containsKey(player);
  }
  
  @Override
  public ShopActionData getAction(@NotNull UUID player) {
    return actionData.get(player);
  }
  
  @Override
  public void setAction(@NotNull UUID player, @NotNull ShopActionData data) {
    actionData.put(player, data);
  }
  
  @Override
  public void removeAction(@NotNull UUID player) {
    actionData.remove(player);
  }

  @Override
  public boolean buy(
      @NotNull Player p,
      @NotNull String message,
      @NotNull BuyerShop shop, int amount,
      @NotNull ShopSnapshot info) {

    // No enough shop space
    int space = shop.getRemainingSpace();
    if (!shop.unlimited() && space < amount) {
      p.sendMessage(
          Shop.getLocaleManager().get("shop-has-no-space", p, "" + space,
              ItemUtils.getItemStackName(shop.stack())));
      return false;
    }

    // Not enough items
    int count = ShopUtils.countStacks(p.getInventory(), shop.stack());
    amount = amount == -1 ? count : amount;
    if (amount > count) {
      p.sendMessage(Shop.getLocaleManager().get("you-dont-have-that-many-items", p, "" + count * info.stack().<ItemStack>stack().getAmount(),
          ItemUtils.getItemStackName(shop.stack())));
      return false;
    }

    // Event
    ShopPurchaseEvent e = new ShopPurchaseEvent(shop, p, amount);
    if (e.isCancelled())
      return false;

    amount = e.getAmount();
    return actionBuy0(p, message, shop, amount, info);
  }

  private boolean actionBuy0(
      @NotNull Player p,
      @NotNull String message,
      @NotNull BuyerShop shop, int amount,
      @NotNull ShopSnapshot info) {

    // Tax handling
    double tax = BaseConfig.taxRate;
    if (tax < 0)
      tax = 0; // Tax was disabled.

    double totalPrice = amount * shop.<Double>price();
    if (QuickShopPermissionManager.instance().has(p, "quickshop.tax")) {
      tax = 0;
      Util.debug("Disable the Tax for player " + p.getName()
      + " cause they have permission quickshop.tax");
    }
    if (shop.moderator().isModerator(p.getUniqueId())) {
      tax = 0; // Is staff or owner, so we won't will take them tax
    }

    // Paying - withdraw owner
    boolean shouldPayOwner = !shop.unlimited() || BaseConfig.payUnlimitedShopOwners;
    if (shouldPayOwner) {
      boolean withdrawOwner = QuickShop.instance().getEconomy().withdraw(shop.getOwner(), totalPrice); // Withdraw owner's money
      if (!withdrawOwner) {
        p.sendMessage(Shop.getLocaleManager().get("the-owner-cant-afford-to-buy-from-you", p,
            format(totalPrice),
            format(QuickShop.instance().getEconomy().getBalance(shop.getOwner()))));
        return false;
      }
    }

    // Paying - giving seller
    double moneyAfterTax = totalPrice * (1 - tax);
    boolean depositSeller = QuickShop.instance().getEconomy().deposit(p.getUniqueId(), moneyAfterTax); // Deposit player's money
    if (!depositSeller) {
      ShopLogger.instance().warning(
          "Failed to deposit the money " + moneyAfterTax + " to player " + p.getName());
      /* Rollback the trade */
      if (shouldPayOwner) {
        if (!QuickShop.instance().getEconomy().deposit(shop.getOwner(), totalPrice)) {
          ShopLogger.instance().warning("Failed to rollback the purchase actions for player "
              + Bukkit.getOfflinePlayer(shop.getOwner()).getName());
        }
      }
      p.sendMessage(Shop.getLocaleManager().get("purchase-failed", p));
      return false;
    }

    // Purchase successfully
    if (tax != 0 && !BaseConfig.taxAccount.isEmpty())
      QuickShop.instance().getEconomy().deposit(Bukkit.getOfflinePlayer(BaseConfig.taxAccount).getUniqueId(), totalPrice * tax);

    int space = shop.getRemainingSpace();
    if (space == amount) {
      String msg = "\n" + Shop.getLocaleManager().get("shop-out-of-space", p, "" + shop.location().x(),
          "" + shop.location().y(), "" + shop.location().z());
      
      if (!shop.unlimited() || !BaseConfig.ignoreUnlimitedMessages)
        Shop.getMessager().send(shop.getOwner(), msg);
    } else {
      String msg = Shop.getLocaleManager().get("player-sold-to-your-store", p, p.getName(),
          String.valueOf(amount), "##########" + Util.serializeItem(shop.stack()) + "##########");
      
      if (!shop.unlimited() || !BaseConfig.ignoreUnlimitedMessages)
        ((QuickShopLocaleManager) QuickShop.instance().getLocaleManager()).sendParsed(p.getUniqueId(), msg, shop.unlimited());
    }
    
    shop.buy(p, amount);
    Shop.getLocaleManager().sendSellSuccess(p, shop, amount);

    ShopSuccessPurchaseEvent se = new ShopSuccessPurchaseEvent(shop, p, amount, totalPrice, tax);
    Bukkit.getPluginManager().callEvent(se);

    ((ChestShop) shop).setSignText(); // Update the signs count\
    return true;
  }

  @Override
  public void create(
      @NotNull Player p,
      @NotNull ShopCreator info,
      @NotNull String message,
      boolean bypassProtectionChecks) {

    Util.debug("actionCreate");
    if (!bypassProtectionChecks) {
      Util.debug("Calling for protection check...");

      QuickShop.instance().getNcpExemptor().toggleProtectionListeners(false, p);
      if (!QuickShop.instance().getPermissionChecker().canBuild(p, info.location())) {
        p.sendMessage(Shop.getLocaleManager().get("no-permission", p)
            + ": Some 3rd party plugin denied the permission checks, did you have permission built in there?");
        Util.debug("Failed to create shop: Protection check failed:");
        for (RegisteredListener belisteners : BlockExpEvent.getHandlerList()
            .getRegisteredListeners()) {
          Util.debug(belisteners.getPlugin().getName());
        }
        return;
      }
      QuickShop.instance().getNcpExemptor().toggleProtectionListeners(true, p);
    }

    if (Shop.getManager().getLoadedShopAt(info.location()).isPresent()) {
      p.sendMessage(Shop.getLocaleManager().get("shop-already-owned", p));
      return;
    }

    if (BlockUtils.getSecondHalf(info.location().block()).isPresent()
        && !QuickShopPermissionManager.instance().has(p, "quickshop.create.double")) {
      p.sendMessage(Shop.getLocaleManager().get("no-double-chests", p));
      return;
    }
    
    if (!ShopUtils.canBeShop(info.location().block())) {
      p.sendMessage(Shop.getLocaleManager().get("chest-was-removed", p));
      return;
    }
    
    if (info.location().block().getType() == Material.ENDER_CHEST) {
      if (!QuickShopPermissionManager.instance().has(p, "quickshop.create.enderchest")) {
        return;
      }
    }

    if (BaseConfig.autoSign && !BaseConfig.allowNoSign) {

      if (info.sign() == null) {
        p.sendMessage(Shop.getLocaleManager().get("failed-to-put-sign", p));
        return;
      }

      Material signType = info.sign().getType();
      if (!BlockUtils.isAir(signType) && signType != Material.WATER) {
        
        boolean wallSign;
        try {
          wallSign = Tag.WALL_SIGNS.isTagged(signType);
        } catch (Throwable t) {
          wallSign = signType.name().equals("WALL_SIGN");
        }
        
        if (!wallSign ||
            !Arrays.stream(
                ((Sign) info.sign().getState()).getLines())
            .allMatch(String::isEmpty)) {
          
          p.sendMessage(Shop.getLocaleManager().get("failed-to-put-sign", p));
          return;
        }
      }
    }

    // Price per item
    double price;
    double minPrice = BaseConfig.minimumPrice == -1 ? Integer.MIN_VALUE : BaseConfig.minimumPrice;

    try {
      if (BaseConfig.integerPriceOnly) {
        try {
          price = Integer.parseInt(message);
        } catch (NumberFormatException ex2) {
          // input is number, but not Integer
          Util.debug(ex2.getMessage());
          p.sendMessage(Shop.getLocaleManager().get("not-a-integer", p, message));
          return;
        }
      } else {
        price = Double.parseDouble(message);
        String strFormat =
            new DecimalFormat("#.#########").format(Math.abs(price)).replace(",", ".");
        String[] processedDouble = strFormat.split(".");
        if (processedDouble.length > 1) {
          int maximumDigitsLimit = BaseConfig.maximumPriceDigitals;
          if (processedDouble[1].length() > maximumDigitsLimit && maximumDigitsLimit != -1) {
            p.sendMessage(Shop.getLocaleManager().get("digits-reach-the-limit", p,
                String.valueOf(maximumDigitsLimit)));
            return;
          }
        }
      }

    } catch (NumberFormatException ex) {
      // No number input
      Util.debug(ex.getMessage());
      p.sendMessage(Shop.getLocaleManager().get("not-a-number", p, message));
      return;
    }

    boolean decFormat = BaseConfig.decimalFormatPrice;
    if (BaseConfig.allowFreeShops) {
      if (price != 0 && price < minPrice) {
        p.sendMessage(Shop.getLocaleManager().get("price-too-cheap", p,
            (decFormat) ? ShopUtils.formatPrice(minPrice) : "" + minPrice));
        return;
      }
    } else {
      if (price < minPrice) {
        p.sendMessage(Shop.getLocaleManager().get("price-too-cheap", p,
            (decFormat) ? ShopUtils.formatPrice(minPrice) : "" + minPrice));
        return;
      }
    }

    double price_limit = BaseConfig.maximumPrice == -1 ? Integer.MAX_VALUE : BaseConfig.maximumPrice;
    if (price_limit != -1) {
      if (price > price_limit) {
        p.sendMessage(Shop.getLocaleManager().get("price-too-high", p,
            (decFormat) ? ShopUtils.formatPrice(price_limit) : "" + price_limit));
        return;
      }
    }

    // Check price restriction
    Entry<Double, Double> priceRestriction = Util.getPriceRestriction(info.stack().<ItemStack>stack().getType());
    if (priceRestriction != null) {
      if (price < priceRestriction.getKey() || price > priceRestriction.getValue()) {
        // p.sendMessage(ChatColor.RED+"Restricted prices for
        // "+info.stack().getType()+": min "+priceRestriction.getKey()+", max
        // "+priceRestriction.getValue());
        p.sendMessage(Shop.getLocaleManager().get("restricted-prices", p,
            ItemUtils.getItemStackName(info.stack().stack()), String.valueOf(priceRestriction.getKey()),
            String.valueOf(priceRestriction.getValue())));
      }
    }
    
    if (!QuickShop.instance().getIntegrationManager().callIntegrationsCanCreate(p, info.location().bukkit())) {
      Util.debug("Cancelled by integrations");
      return;
    }
    
    if (!BaseConfig.lock) {
      p.sendMessage(Shop.getLocaleManager().get("shops-arent-locked", p));
    }
    
    /*
     * Creates the shop
     */
    ContainerQuickShop shop = new QuickShopSeller(info.location(), price, info.stack().stack(),
        new ShopModerator(p.getUniqueId()), false, ShopType.SELLING);

    ShopCreateEvent e = new ShopCreateEvent(shop, p);
    if (Util.fireCancellableEvent(e))
      return;
    
    double createCost = BaseConfig.createCost;

    // This must be called after the event has been called.
    // Else, if the event is cancelled, they won't get their
    // money back.
    if (QuickShopPermissionManager.instance().has(p, "quickshop.bypasscreatefee")) {
      createCost = 0;
    }

    if (createCost > 0) {
      if (!QuickShop.instance().getEconomy().withdraw(p.getUniqueId(), createCost)) {
        p.sendMessage(Shop.getLocaleManager().get("you-cant-afford-a-new-shop", p,
            format(createCost)));
        return;
      }
      try {
        String taxAccount = BaseConfig.taxAccount;
        if (taxAccount != null) {
          QuickShop.instance().getEconomy().deposit(Bukkit.getOfflinePlayer(taxAccount).getUniqueId(),
              createCost);
        }
      } catch (Exception e2) {
        e2.printStackTrace();
        ShopLogger.instance().log(Level.WARNING,
            "QuickShop can't pay tax to account in config.yml, Please set tax account name to a existing player!");
      }
    }

    /* The shop has hereforth been successfully created */
    Shop.getManager().createShop(shop, info);
    // Figures out which way we should put the sign on and
    // sets its text.

    if (shop.isDualShop()) {
      ChestShop nextTo = shop.getAttachedShop();
      if (nextTo.<Double>price() > shop.price()) {
        // The one next to it must always be a
        // buying shop.
        p.sendMessage(Shop.getLocaleManager().get("buying-more-than-selling", p));
      }
    }
  }

  @Override
  public void sell(
      @NotNull Player p,
      @NotNull String message,
      @NotNull SellerShop shop, int amount,
      @NotNull ShopSnapshot info) {

    int stock = shop.getRemainingStock();
    
    amount = amount == -1 ? stock : amount;
    stock = shop.unlimited() ? Integer.MAX_VALUE : stock;
    String stacks = info.stack().<ItemStack>stack().getAmount() > 1 ? " * " + info.stack().<ItemStack>stack().getAmount() : "";
    if (stock < amount) {
      p.sendMessage(Shop.getLocaleManager().get("shop-stock-too-low", p, String.valueOf(stock),
          ItemUtils.getItemStackName(shop.stack()) + stacks));
      return;
    }
    if (amount < 1) {
      // & Dumber
      p.sendMessage(Shop.getLocaleManager().get("negative-amount", p));
      return;
    }
    int pSpace = ShopUtils.countSpace(p.getInventory(), shop.stack());
    if (amount > pSpace) {
      p.sendMessage(Shop.getLocaleManager().get("not-enough-space", p, String.valueOf(pSpace)));
      return;
    }
    ShopPurchaseEvent e = new ShopPurchaseEvent(shop, p, amount);
    if (Util.fireCancellableEvent(e)) {
      return; // Cancelled
    }
    // Money handling
    double tax = BaseConfig.taxRate;
    double total = amount * shop.<Double>price();
    if (QuickShopPermissionManager.instance().has(p, "quickshop.tax")) {
      tax = 0;
      Util.debug("Disable the Tax for player " + p.getName()
      + " cause they have permission quickshop.tax");
    }
    if (tax < 0) {
      tax = 0; // Tax was disabled.
    }
    if (shop.moderator().isModerator(p.getUniqueId())) {
      tax = 0; // Is staff or owner, so we won't will take them tax
    }

    boolean successA = QuickShop.instance().getEconomy().withdraw(p.getUniqueId(), total); // Withdraw owner's money
    if (!successA) {
      p.sendMessage(
          Shop.getLocaleManager().get("you-cant-afford-to-buy", p, Objects.requireNonNull(format(total)),
              Objects.requireNonNull(format(QuickShop.instance().getEconomy().getBalance(p.getUniqueId())))));
      return;
    }
    boolean shouldPayOwner = !shop.unlimited() || BaseConfig.payUnlimitedShopOwners;
    if (shouldPayOwner) {
      double depositMoney = total * (1 - tax);
      boolean successB = QuickShop.instance().getEconomy().deposit(shop.getOwner(), depositMoney);
      if (!successB) {
        ShopLogger.instance().warning(
            "Failed to deposit the money for player " + Bukkit.getOfflinePlayer(shop.getOwner()));
        /* Rollback the trade */
        if (!QuickShop.instance().getEconomy().deposit(p.getUniqueId(), depositMoney)) {
          ShopLogger.instance().warning("Failed to rollback the purchase actions for player "
              + Bukkit.getOfflinePlayer(shop.getOwner()).getName());
        }
        p.sendMessage(Shop.getLocaleManager().get("purchase-failed", p));
        return;
      }
    }

    String msg;
    // Notify the shop owner
    if (BaseConfig.showTax) {
      msg = Shop.getLocaleManager().get("player-bought-from-your-store-tax", p, p.getName(), "" + amount,
          "##########" + Util.serializeItem(shop.stack()) + "##########", JavaUtils.format((tax * total)));
    } else {
      msg = Shop.getLocaleManager().get("player-bought-from-your-store", p, p.getName(), "" + amount,
          "##########" + Util.serializeItem(shop.stack()) + "##########");
    }
    // Transfers the item from A to B
    if (stock == amount)
      msg += "\n" + Shop.getLocaleManager().get("shop-out-of-stock", p, "" + shop.location().x(),
          "" + shop.location().y(), "" + shop.location().z(),
          ItemUtils.getItemStackName(shop.stack()));
    
    ((QuickShopLocaleManager) QuickShop.instance().getLocaleManager()).sendParsed(shop.getOwner(), msg, shop.unlimited());
    
    shop.sell(p, amount);
    Shop.getLocaleManager().sendPurchaseSuccess(p, shop, amount, info);
    ShopSuccessPurchaseEvent se = new ShopSuccessPurchaseEvent(shop, p, amount, total, tax);
    Bukkit.getPluginManager().callEvent(se);
  }

  @Override
  public void trade(
      @NotNull Player p,
      @NotNull ShopSnapshot info,
      @NotNull String message) {

    if (QuickShop.instance().getEconomy() == null) {
      p.sendMessage("Error: Economy system not loaded, type /qs main command to get details.");
      return;
    }

    if (!QuickShop.instance().getIntegrationManager().callIntegrationsCanTrade(p, info.location().bukkit())) {
      Util.debug("Cancel by integrations.");
      return;
    }

    // Shop gone
    // Get the shop they interacted with
    ShopViewer shopOp = Shop.getManager().getLoadedShopAt(info.location());
    // It's not valid anymore
    if (!shopOp.isPresent() || !ShopUtils.canBeShop(info.location().block())) {
      p.sendMessage(Shop.getLocaleManager().get("chest-was-removed", p));
      return;
    }

    // Shop changed
    ChestShop shop = shopOp.get();
    if (info.hasChanged(shop)) {
      p.sendMessage(Shop.getLocaleManager().get("shop-has-changed", p));
      return;
    }

    int amount;
    try {
      amount = Integer.parseInt(message);

      // Negative amount
      if (amount < 1) {
        p.sendMessage(Shop.getLocaleManager().get("negative-amount", p));
        return;
      }
    } catch (NumberFormatException e) {
      if (message.equalsIgnoreCase(BaseConfig.tradeAllWord)) {
        amount = -1;
      } else {
        p.sendMessage(Shop.getLocaleManager().get("not-a-integer", p, message));
        Util.debug("Receive the chat " + message + " and it format failed: " + e.getMessage());
        return;
      }
    }

    if (shop instanceof BuyerShop) {
      buy(p, message, (BuyerShop) shop, amount, info);
    } else if (shop instanceof SellerShop) {
      sell(p, message, (SellerShop) shop, amount, info);
    }
  }

  @Override
  public void handleChat(@NotNull Player p, @NotNull String msg, boolean sync) {
    handleChat(p, msg, false, sync);
  }

  @Override
  public void handleChat(@NotNull Player p, @NotNull String msg, boolean bypassProtectionChecks, boolean sync) {
    final String message = ChatColor.stripColor(msg);
    Runnable runnable = () -> {
      // They wanted to do something.
      ShopActionData info = actionData.remove(p.getUniqueId());
      
      ShopLogger.instance().warning("info: " + info);
      if (!info.location().worldName().equals(p.getLocation().getWorld().getName())
          || info.location().bukkit().distanceSquared(p.getLocation()) > 25) {
        p.sendMessage(Shop.getLocaleManager().get("not-looking-at-shop", p));
        return;
      }
      
      if (info.action() == ShopAction.CREATE) {
        create(p, (ShopCreator) info, message, bypassProtectionChecks);
      }
      if (info.action() == ShopAction.TRADE) {
        trade(p, (ShopSnapshot) info, message);
      }
    };
    
    if (sync)
      Bukkit.getScheduler().runTask(QuickShop.instance(), () -> {
        try {
          runnable.run();
        } catch (Throwable t) {
          t.printStackTrace();
        }
      });
    else
      runnable.run();
  }
  
  /**
   * Format the price use economy system
   *
   * @param d price
   * @return formated price
   */
  public @Nullable String format(double d) {
    return QuickShop.instance().getEconomy().format(d);
  }

  @Override
  public void clear() {
    actionData.clear();
  }
}
