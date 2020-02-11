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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import com.google.common.collect.Maps;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopActionManager;
import cc.bukkit.shop.ShopModerator;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.data.ShopAction;
import cc.bukkit.shop.data.ShopActionData;
import cc.bukkit.shop.data.ShopCreator;
import cc.bukkit.shop.data.ShopSnapshot;
import cc.bukkit.shop.event.ShopCreateEvent;
import cc.bukkit.shop.event.ShopPurchaseEvent;
import cc.bukkit.shop.event.ShopSuccessPurchaseEvent;
import cc.bukkit.shop.util.ShopLogger;
import cc.bukkit.shop.viewer.ShopViewer;

public class QuickShopActionManager implements ShopActionManager {
  // TODO delayed remove
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
  public void setAction(@NotNull UUID player, @NotNull ShopActionData data) {
    actionData.put(player, data);
  }
  
  @Override
  public void removeAction(@NotNull UUID player) {
    actionData.remove(player);
  }

  @Override
  public boolean actionBuy(
      @NotNull Player p,
      @NotNull String message,
      @NotNull ContainerShop shop, int amount,
      @NotNull ShopSnapshot info) {

    // No enough shop space
    int space = shop.getRemainingSpace();
    if (!shop.isUnlimited() && space < amount) {
      p.sendMessage(
          MsgUtil.getMessage("shop-has-no-space", p, "" + space,
              Util.getItemStackName(shop.getItem())));
      return false;
    }

    // Not enough items
    int count = Util.countStacks(p.getInventory(), shop.getItem());
    amount = amount == -1 ? count : amount;
    if (amount > count) {
      p.sendMessage(MsgUtil.getMessage("you-dont-have-that-many-items", p, "" + count * info.item().getAmount(),
          Util.getItemStackName(shop.getItem())));
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
      @NotNull ContainerShop shop, int amount,
      @NotNull ShopSnapshot info) {

    // Tax handling
    double tax = BaseConfig.taxRate;
    if (tax < 0)
      tax = 0; // Tax was disabled.

    double totalPrice = amount * shop.getPrice();
    if (PermissionManager.instance().has(p, "quickshop.tax")) {
      tax = 0;
      Util.debug("Disable the Tax for player " + p.getName()
      + " cause they have permission quickshop.tax");
    }
    if (shop.getModerator().isModerator(p.getUniqueId())) {
      tax = 0; // Is staff or owner, so we won't will take them tax
    }

    // Paying - withdraw owner
    boolean shouldPayOwner = !shop.isUnlimited() || BaseConfig.payUnlimitedShopOwners;
    if (shouldPayOwner) {
      boolean withdrawOwner = QuickShop.instance().getEconomy().withdraw(shop.getOwner(), totalPrice); // Withdraw owner's money
      if (!withdrawOwner) {
        p.sendMessage(MsgUtil.getMessage("the-owner-cant-afford-to-buy-from-you", p,
            Objects.requireNonNull(format(totalPrice)),
            Objects.requireNonNull(format(QuickShop.instance().getEconomy().getBalance(shop.getOwner())))));
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
      p.sendMessage(MsgUtil.getMessage("purchase-failed", p));
      return false;
    }

    // Purchase successfully
    if (tax != 0 && !BaseConfig.taxAccount.isEmpty())
      QuickShop.instance().getEconomy().deposit(Bukkit.getOfflinePlayer(BaseConfig.taxAccount).getUniqueId(), totalPrice * tax);

    // Notify the owner of the purchase.
    String msg = MsgUtil.getMessage("player-sold-to-your-store", p, p.getName(),
        String.valueOf(amount), "##########" + Util.serialize(shop.getItem()) + "##########");

    int space = shop.getRemainingSpace();
    if (space == amount) {
      msg += "\n" + MsgUtil.getMessage("shop-out-of-space", p, "" + shop.getLocation().x(),
          "" + shop.getLocation().y(), "" + shop.getLocation().z());
    }

    if (!shop.isUnlimited() || !BaseConfig.ignoreUnlimitedMessages)
      MsgUtil.send(shop.getOwner(), msg);
    
    shop.buy(p, amount);
    MsgUtil.sendSellSuccess(p, shop, amount);

    ShopSuccessPurchaseEvent se = new ShopSuccessPurchaseEvent(shop, p, amount, totalPrice, tax);
    Bukkit.getPluginManager().callEvent(se);

    shop.setSignText(); // Update the signs count\
    return true;
  }

  @Override
  public void actionCreate(
      @NotNull Player p,
      @NotNull ShopCreator info,
      @NotNull String message,
      boolean bypassProtectionChecks) {

    Util.debug("actionCreate");
    if (!bypassProtectionChecks) {
      Util.debug("Calling for protection check...");

      QuickShop.instance().getCompatibilityTool().toggleProtectionListeners(false, p);
      if (!QuickShop.instance().getPermissionChecker().canBuild(p, info.location())) {
        p.sendMessage(MsgUtil.getMessage("no-permission", p)
            + ": Some 3rd party plugin denied the permission checks, did you have permission built in there?");
        Util.debug("Failed to create shop: Protection check failed:");
        for (RegisteredListener belisteners : BlockExpEvent.getHandlerList()
            .getRegisteredListeners()) {
          Util.debug(belisteners.getPlugin().getName());
        }
        return;
      }
      QuickShop.instance().getCompatibilityTool().toggleProtectionListeners(true, p);
    }

    if (Shop.getManager().getLoadedShopAt(info.location()).isPresent()) {
      p.sendMessage(MsgUtil.getMessage("shop-already-owned", p));
      return;
    }

    if (Util.getSecondHalf(info.location().block()).isPresent()
        && !PermissionManager.instance().has(p, "quickshop.create.double")) {
      p.sendMessage(MsgUtil.getMessage("no-double-chests", p));
      return;
    }
    
    if (!Util.canBeShop(info.location().block())) {
      p.sendMessage(MsgUtil.getMessage("chest-was-removed", p));
      return;
    }
    
    if (info.location().block().getType() == Material.ENDER_CHEST) {
      if (!PermissionManager.instance().has(p, "quickshop.create.enderchest")) {
        return;
      }
    }

    if (BaseConfig.autoSign && !BaseConfig.allowNoSign) {

      if (info.sign() == null) {
        p.sendMessage(MsgUtil.getMessage("failed-to-put-sign", p));
        return;
      }

      Material signType = info.sign().getType();
      if (!Util.isAir(signType) && signType != Material.WATER) {
        
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
          
          p.sendMessage(MsgUtil.getMessage("failed-to-put-sign", p));
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
          p.sendMessage(MsgUtil.getMessage("not-a-integer", p, message));
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
            p.sendMessage(MsgUtil.getMessage("digits-reach-the-limit", p,
                String.valueOf(maximumDigitsLimit)));
            return;
          }
        }
      }

    } catch (NumberFormatException ex) {
      // No number input
      Util.debug(ex.getMessage());
      p.sendMessage(MsgUtil.getMessage("not-a-number", p, message));
      return;
    }

    boolean decFormat = BaseConfig.decimalFormatPrice;
    if (BaseConfig.allowFreeShops) {
      if (price != 0 && price < minPrice) {
        p.sendMessage(MsgUtil.getMessage("price-too-cheap", p,
            (decFormat) ? MsgUtil.decimalFormat(minPrice) : "" + minPrice));
        return;
      }
    } else {
      if (price < minPrice) {
        p.sendMessage(MsgUtil.getMessage("price-too-cheap", p,
            (decFormat) ? MsgUtil.decimalFormat(minPrice) : "" + minPrice));
        return;
      }
    }

    double price_limit = BaseConfig.maximumPrice == -1 ? Integer.MAX_VALUE : BaseConfig.maximumPrice;
    if (price_limit != -1) {
      if (price > price_limit) {
        p.sendMessage(MsgUtil.getMessage("price-too-high", p,
            (decFormat) ? MsgUtil.decimalFormat(price_limit) : "" + price_limit));
        return;
      }
    }

    // Check price restriction
    Entry<Double, Double> priceRestriction = Util.getPriceRestriction(info.item().getType());
    if (priceRestriction != null) {
      if (price < priceRestriction.getKey() || price > priceRestriction.getValue()) {
        // p.sendMessage(ChatColor.RED+"Restricted prices for
        // "+info.getItem().getType()+": min "+priceRestriction.getKey()+", max
        // "+priceRestriction.getValue());
        p.sendMessage(MsgUtil.getMessage("restricted-prices", p,
            Util.getItemStackName(info.item()), String.valueOf(priceRestriction.getKey()),
            String.valueOf(priceRestriction.getValue())));
      }
    }
    
    Util.debug("232");
    
    if (!QuickShop.instance().getIntegrationHelper().callIntegrationsCanCreate(p, info.location().bukkit())) {
      Util.debug("Cancelled by integrations");
      return;
    }
    
    if (!BaseConfig.lock) {
      p.sendMessage(MsgUtil.getMessage("shops-arent-locked", p));
    }
    
    /*
     * Creates the shop
     */
    ContainerQuickShop shop = new ContainerQuickShop(info.location(), price, info.item(),
        new ShopModerator(p.getUniqueId()), false, ShopType.SELLING);

    ShopCreateEvent e = new ShopCreateEvent(shop, p);
    if (Util.fireCancellableEvent(e))
      return;
    
    double createCost = BaseConfig.createCost;

    // This must be called after the event has been called.
    // Else, if the event is cancelled, they won't get their
    // money back.
    if (PermissionManager.instance().has(p, "quickshop.bypasscreatefee")) {
      createCost = 0;
    }

    if (createCost > 0) {
      if (!QuickShop.instance().getEconomy().withdraw(p.getUniqueId(), createCost)) {
        p.sendMessage(MsgUtil.getMessage("you-cant-afford-a-new-shop", p,
            Objects.requireNonNull(format(createCost))));
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
      ContainerShop nextTo = shop.getAttachedShop();
      if (Objects.requireNonNull(nextTo).getPrice() > shop.getPrice()) {
        // The one next to it must always be a
        // buying shop.
        p.sendMessage(MsgUtil.getMessage("buying-more-than-selling", p));
      }
    }
  }

  @Override
  public void actionSell(
      @NotNull Player p,
      @NotNull String message,
      @NotNull ContainerShop shop, int amount,
      @NotNull ShopSnapshot info) {

    int stock = shop.getRemainingStock();
    amount = amount == -1 ? stock : amount;
    stock = shop.isUnlimited() ? Integer.MAX_VALUE : stock;
    String stacks = info.item().getAmount() > 1 ? " * " + info.item().getAmount() : "";
    if (stock < amount) {
      p.sendMessage(MsgUtil.getMessage("shop-stock-too-low", p, String.valueOf(stock),
          Util.getItemStackName(shop.getItem()) + stacks));
      return;
    }
    if (amount < 1) {
      // & Dumber
      p.sendMessage(MsgUtil.getMessage("negative-amount", p));
      return;
    }
    int pSpace = Util.countSpace(p.getInventory(), shop.getItem());
    if (amount > pSpace) {
      p.sendMessage(MsgUtil.getMessage("not-enough-space", p, String.valueOf(pSpace)));
      return;
    }
    ShopPurchaseEvent e = new ShopPurchaseEvent(shop, p, amount);
    if (Util.fireCancellableEvent(e)) {
      return; // Cancelled
    }
    // Money handling
    double tax = BaseConfig.taxRate;
    double total = amount * shop.getPrice();
    if (PermissionManager.instance().has(p, "quickshop.tax")) {
      tax = 0;
      Util.debug("Disable the Tax for player " + p.getName()
      + " cause they have permission quickshop.tax");
    }
    if (tax < 0) {
      tax = 0; // Tax was disabled.
    }
    if (shop.getModerator().isModerator(p.getUniqueId())) {
      tax = 0; // Is staff or owner, so we won't will take them tax
    }

    boolean successA = QuickShop.instance().getEconomy().withdraw(p.getUniqueId(), total); // Withdraw owner's money
    if (!successA) {
      p.sendMessage(
          MsgUtil.getMessage("you-cant-afford-to-buy", p, Objects.requireNonNull(format(total)),
              Objects.requireNonNull(format(QuickShop.instance().getEconomy().getBalance(p.getUniqueId())))));
      return;
    }
    boolean shouldPayOwner = !shop.isUnlimited() || BaseConfig.payUnlimitedShopOwners;
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
        p.sendMessage(MsgUtil.getMessage("purchase-failed", p));
        return;
      }
    }

    String msg;
    // Notify the shop owner
    if (BaseConfig.showTax) {
      msg = MsgUtil.getMessage("player-bought-from-your-store-tax", p, p.getName(), "" + amount,
          "##########" + Util.serialize(shop.getItem()) + "##########", Util.format((tax * total)));
    } else {
      msg = MsgUtil.getMessage("player-bought-from-your-store", p, p.getName(), "" + amount,
          "##########" + Util.serialize(shop.getItem()) + "##########");
    }
    // Transfers the item from A to B
    if (stock == amount) {
      msg += "\n" + MsgUtil.getMessage("shop-out-of-stock", p, "" + shop.getLocation().x(),
          "" + shop.getLocation().y(), "" + shop.getLocation().z(),
          Util.getItemStackName(shop.getItem()));
    }

    if (!shop.isUnlimited() || !BaseConfig.ignoreUnlimitedMessages)
      MsgUtil.send(shop.getOwner(), msg);
    
    shop.sell(p, amount);
    MsgUtil.sendPurchaseSuccess(p, shop, amount, info);
    ShopSuccessPurchaseEvent se = new ShopSuccessPurchaseEvent(shop, p, amount, total, tax);
    Bukkit.getPluginManager().callEvent(se);
  }

  @Override
  public void actionTrade(
      @NotNull Player p,
      @NotNull ShopSnapshot info,
      @NotNull String message) {

    if (QuickShop.instance().getEconomy() == null) {
      p.sendMessage("Error: Economy system not loaded, type /qs main command to get details.");
      return;
    }

    if (!QuickShop.instance().getIntegrationHelper().callIntegrationsCanTrade(p, info.location().bukkit())) {
      Util.debug("Cancel by integrations.");
      return;
    }

    // Shop gone
    // Get the shop they interacted with
    ShopViewer shopOp = Shop.getManager().getLoadedShopAt(info.location());
    // It's not valid anymore
    if (!shopOp.isPresent() || !Util.canBeShop(info.location().block())) {
      p.sendMessage(MsgUtil.getMessage("chest-was-removed", p));
      return;
    }

    // Shop changed
    ContainerShop shop = shopOp.get();
    if (info.hasChanged(shop)) {
      p.sendMessage(MsgUtil.getMessage("shop-has-changed", p));
      return;
    }

    int amount;
    try {
      amount = Integer.parseInt(message);

      // Negative amount
      if (amount < 1) {
        p.sendMessage(MsgUtil.getMessage("negative-amount", p));
        return;
      }
    } catch (NumberFormatException e) {
      if (message.equalsIgnoreCase(BaseConfig.tradeAllWord)) {
        amount = -1;
      } else {
        p.sendMessage(MsgUtil.getMessage("not-a-integer", p, message));
        Util.debug("Receive the chat " + message + " and it format failed: " + e.getMessage());
        return;
      }
    }

    if (shop.getShopType() == ShopType.BUYING) {
      actionBuy(p, message, shop, amount, info);
    } else {
      actionSell(p, message, shop, amount, info);
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
      
      if (!info.location().worldName().equals(p.getLocation().getWorld().getName())
          || info.location().bukkit().distanceSquared(p.getLocation()) > 25) {
        p.sendMessage(MsgUtil.getMessage("not-looking-at-shop", p));
        return;
      }
      
      if (info.action() == ShopAction.CREATE) {
        actionCreate(p, (ShopCreator) info, message, bypassProtectionChecks);
      }
      if (info.action() == ShopAction.TRADE) {
        actionTrade(p, (ShopSnapshot) info, message);
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

  /** @return Returns the HashMap. Info contains what their last question etc was. */
  public Map<UUID, ShopActionData> getActions() {
    return this.actionData;
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
