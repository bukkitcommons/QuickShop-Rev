package org.maxgamer.quickshop.shop.impl;

import com.google.common.collect.Sets;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.economy.Economy;
import org.maxgamer.quickshop.event.ShopCreateEvent;
import org.maxgamer.quickshop.event.ShopPreCreateEvent;
import org.maxgamer.quickshop.event.ShopPurchaseEvent;
import org.maxgamer.quickshop.event.ShopSuccessPurchaseEvent;
import org.maxgamer.quickshop.shop.Info;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.ShopAction;
import org.maxgamer.quickshop.shop.ShopChunk;
import org.maxgamer.quickshop.shop.ShopModerator;
import org.maxgamer.quickshop.shop.ShopType;
import org.maxgamer.quickshop.utils.MsgUtil;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.viewer.ShopViewer;

/** Manage a lot of shops. */
public class ShopManager {

  private final HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>> shops =
      new HashMap<>();
  private final Set<Shop> loadedShops =
      QuickShop.instance().isEnabledAsyncDisplayDespawn() ? Sets.newConcurrentHashSet()
          : Sets.newHashSet();
      private HashMap<UUID, Info> actions = new HashMap<>();

      private boolean actionBuy(
          @NotNull Player p,
          @NotNull Economy eco,
          @NotNull HashMap<UUID, Info> actions2,
          @NotNull Info info,
          @NotNull String message,
          @NotNull Shop shop,
          int amount) {

        // No enough shop space
        int space = shop.getRemainingSpace();
        if (space != -1 && space < amount) {
          p.sendMessage(
              MsgUtil.getMessage("shop-has-no-space", p, "" + space,
                  Util.getItemStackName(shop.getItem())));
          return false;
        }

        // Not enough items
        int count = Util.countItems(p.getInventory(), shop.getItem());
        if (amount > count) {
          p.sendMessage(MsgUtil.getMessage("you-dont-have-that-many-items", p, "" + count,
              Util.getItemStackName(shop.getItem())));
          return false;
        }

        // Event
        ShopPurchaseEvent e = new ShopPurchaseEvent(shop, p, amount);
        if (e.isCancelled())
          return false;

        amount = e.getAmount();
        // FIXME event modify

        return actionBuy0(p, eco, actions2, info, message, shop, amount);
      }

      private boolean actionBuy0(
          @NotNull Player p,
          @NotNull Economy eco,
          @NotNull HashMap<UUID, Info> actions2,
          @NotNull Info info,
          @NotNull String message,
          @NotNull Shop shop,
          int amount) {

        // Tax handling
        double tax = BaseConfig.taxRate;
        if (tax < 0)
          tax = 0; // Tax was disabled.

        double totalPrice = amount * shop.getPrice();
        if (QuickShop.getPermissionManager().hasPermission(p, "quickshop.tax")) {
          tax = 0;
          Util.debugLog("Disable the Tax for player " + p.getName()
          + " cause they have permission quickshop.tax");
        }
        if (shop.getModerator().isModerator(p.getUniqueId())) {
          tax = 0; // Is staff or owner, so we won't will take them tax
        }

        // Paying - withdraw owner
        boolean shouldPayOwner =
            !shop.isUnlimited() ||
            QuickShop.instance().getConfig().getBoolean("shop.pay-unlimited-shop-owners");
        if (shouldPayOwner) {
          boolean withdrawOwner = eco.withdraw(shop.getOwner(), totalPrice); // Withdraw owner's money
          if (!withdrawOwner) {
            p.sendMessage(MsgUtil.getMessage("the-owner-cant-afford-to-buy-from-you", p,
                Objects.requireNonNull(format(totalPrice)),
                Objects.requireNonNull(format(eco.getBalance(shop.getOwner())))));
            return false;
          }
        }

        // Paying - giving seller
        double moneyAfterTax = totalPrice * (1 - tax);
        boolean depositSeller = eco.deposit(p.getUniqueId(), moneyAfterTax); // Deposit player's money
        if (!depositSeller) {
          QuickShop.instance().getLogger().warning(
              "Failed to deposit the money " + moneyAfterTax + " to player " + p.getName());
          /* Rollback the trade */
          if (shouldPayOwner) {
            if (!eco.deposit(shop.getOwner(), totalPrice)) {
              QuickShop.instance().getLogger().warning("Failed to rollback the purchase actions for player "
                  + Bukkit.getOfflinePlayer(shop.getOwner()).getName());
            }
          }
          p.sendMessage(MsgUtil.getMessage("purchase-failed", p));
          return false;
        }

        // Purchase successfully
        if (tax != 0 && !BaseConfig.taxAccount.isEmpty())
          eco.deposit(Bukkit.getOfflinePlayer(BaseConfig.taxAccount).getUniqueId(), totalPrice * tax);

        // Notify the owner of the purchase.
        String msg = MsgUtil.getMessage("player-sold-to-your-store", p, p.getName(),
            String.valueOf(amount), "##########" + Util.serialize(shop.getItem()) + "##########");

        int space = shop.getRemainingSpace();
        if (space == amount) {
          msg += "\n" + MsgUtil.getMessage("shop-out-of-space", p, "" + shop.getLocation().getBlockX(),
              "" + shop.getLocation().getBlockY(), "" + shop.getLocation().getBlockZ());
        }

        MsgUtil.send(shop.getOwner(), msg, shop.isUnlimited());
        shop.buy(p, amount);
        MsgUtil.sendSellSuccess(p, shop, amount);

        ShopSuccessPurchaseEvent se = new ShopSuccessPurchaseEvent(shop, p, amount, totalPrice, tax);
        Bukkit.getPluginManager().callEvent(se);

        shop.setSignText(); // Update the signs count\
        return true;
      }

      private void actionCreate(
          @NotNull Player p,
          @NotNull HashMap<UUID, Info> actions2,
          @NotNull Info info,
          @NotNull String message,
          boolean bypassProtectionChecks) {

        Util.debugLog("actionCreate");
        if (!bypassProtectionChecks) {
          Util.debugLog("Calling for protection check...");

          QuickShop.instance().getCompatibilityTool().toggleProtectionListeners(false, p);
          if (!QuickShop.instance().getPermissionChecker().canBuild(p, info.getLocation())) {
            p.sendMessage(MsgUtil.getMessage("no-permission", p)
                + ": Some 3rd party plugin denied the permission checks, did you have permission built in there?");
            Util.debugLog("Failed to create shop: Protection check failed:");
            for (RegisteredListener belisteners : BlockBreakEvent.getHandlerList()
                .getRegisteredListeners()) {
              Util.debugLog(belisteners.getPlugin().getName());
            }
            return;
          }
          QuickShop.instance().getCompatibilityTool().toggleProtectionListeners(true, p);
        }

        if (getShop(info.getLocation()).isPresent()) {
          p.sendMessage(MsgUtil.getMessage("shop-already-owned", p));
          return;
        }
        
        if (Util.getSecondHalf(info.getLocation().getBlock()) != null
            && !QuickShop.getPermissionManager().hasPermission(p, "quickshop.create.double")) {
          p.sendMessage(MsgUtil.getMessage("no-double-chests", p));
          return;
        }
        if (!Util.canBeShop(info.getLocation().getBlock())) {
          p.sendMessage(MsgUtil.getMessage("chest-was-removed", p));
          return;
        }
        if (info.getLocation().getBlock().getType() == Material.ENDER_CHEST) {
          if (!QuickShop.getPermissionManager().hasPermission(p, "quickshop.create.enderchest")) {
            return;
          }
        }

        // allow-shop-without-space-for-sign check
        if (QuickShop.instance().getConfig().getBoolean("shop.auto-sign")
            && !QuickShop.instance().getConfig().getBoolean("allow-shop-without-space-for-sign")) {

          if (info.getSignBlock() == null) {
            p.sendMessage(MsgUtil.getMessage("failed-to-put-sign", p));
            return;
          }

          Material signType = info.getSignBlock().getType();
          if (!Util.isAir(signType) && signType != Material.WATER) {
            p.sendMessage(MsgUtil.getMessage("failed-to-put-sign", p));
            return;
          }
        }

        // Price per item
        double price;
        double minPrice = QuickShop.instance().getConfig().getDouble("shop.minimum-price");

        try {
          if (QuickShop.instance().getConfig().getBoolean("whole-number-prices-only")) {
            try {
              price = Integer.parseInt(message);
            } catch (NumberFormatException ex2) {
              // input is number, but not Integer
              Util.debugLog(ex2.getMessage());
              p.sendMessage(MsgUtil.getMessage("not-a-integer", p, message));
              return;
            }
          } else {
            price = Double.parseDouble(message);
            String strFormat =
                new DecimalFormat("#.#########").format(Math.abs(price)).replace(",", ".");
            String[] processedDouble = strFormat.split(".");
            if (processedDouble.length > 1) {
              int maximumDigitsLimit = QuickShop.instance().getConfig().getInt("maximum-digits-in-price", -1);
              if (processedDouble[1].length() > maximumDigitsLimit && maximumDigitsLimit != -1) {
                p.sendMessage(MsgUtil.getMessage("digits-reach-the-limit", p,
                    String.valueOf(maximumDigitsLimit)));
                return;
              }
            }
          }

        } catch (NumberFormatException ex) {
          // No number input
          Util.debugLog(ex.getMessage());
          p.sendMessage(MsgUtil.getMessage("not-a-number", p, message));
          return;
        }

        boolean decFormat = QuickShop.instance().getConfig().getBoolean("use-decimal-format");
        if (QuickShop.instance().getConfig().getBoolean("shop.allow-free-shop")) {
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

        double price_limit = QuickShop.instance().getConfig().getInt("shop.maximum-price");
        if (price_limit != -1) {
          if (price > price_limit) {
            p.sendMessage(MsgUtil.getMessage("price-too-high", p,
                (decFormat) ? MsgUtil.decimalFormat(price_limit) : "" + price_limit));
            return;
          }
        }

        // Check price restriction
        Entry<Double, Double> priceRestriction = Util.getPriceRestriction(info.getItem().getType());
        if (priceRestriction != null) {
          if (price < priceRestriction.getKey() || price > priceRestriction.getValue()) {
            // p.sendMessage(ChatColor.RED+"Restricted prices for
            // "+info.getItem().getType()+": min "+priceRestriction.getKey()+", max
            // "+priceRestriction.getValue());
            p.sendMessage(MsgUtil.getMessage("restricted-prices", p,
                Util.getItemStackName(info.getItem()), String.valueOf(priceRestriction.getKey()),
                String.valueOf(priceRestriction.getValue())));
          }
        }

        double createCost = QuickShop.instance().getConfig().getDouble("shop.cost");
        // Create the sample shop.
        ContainerShop shop = new ContainerShop(info.getLocation(), price, info.getItem(),
            new ShopModerator(p.getUniqueId()), false, ShopType.SELLING);

        // This must be called after the event has been called.
        // Else, if the event is cancelled, they won't get their
        // money back.
        if (QuickShop.getPermissionManager().hasPermission(p, "quickshop.bypasscreatefee")) {
          createCost = 0;
        }

        if (createCost > 0) {
          if (!QuickShop.instance().getEconomy().withdraw(p.getUniqueId(), createCost)) {
            p.sendMessage(MsgUtil.getMessage("you-cant-afford-a-new-shop", p,
                Objects.requireNonNull(format(createCost))));
            return;
          }
          try {
            String taxAccount = QuickShop.instance().getConfig().getString("tax-account");
            if (taxAccount != null) {
              QuickShop.instance().getEconomy().deposit(Bukkit.getOfflinePlayer(taxAccount).getUniqueId(),
                  createCost);
            }
          } catch (Exception e2) {
            e2.printStackTrace();
            QuickShop.instance().getLogger().log(Level.WARNING,
                "QuickShop can't pay tax to account in config.yml, Please set tax account name to a existing player!");
          }
        }

        shop.onLoad();
        ShopCreateEvent e = new ShopCreateEvent(shop, p);
        if (Util.fireCancellableEvent(e)) {
          shop.onUnload();
          return;
        }
        if (!QuickShop.instance().getIntegrationHelper().callIntegrationsCanCreate(p, info.getLocation())) {
          shop.onUnload();
          Util.debugLog("Cancelled by integrations");
          return;
        }

        /* The shop has hereforth been successfully created */
        createShop(shop, info);
        if (!QuickShop.instance().getConfig().getBoolean("shop.lock")) {
          // Warn them if they haven't been warned since
          // reboot
          if (!QuickShop.instance().getWarnings().contains(p.getName())) {
            p.sendMessage(MsgUtil.getMessage("shops-arent-locked", p));
            QuickShop.instance().getWarnings().add(p.getName());
          }
        }
        // Figures out which way we should put the sign on and
        // sets its text.

        if (shop.isDoubleShop()) {
          Shop nextTo = shop.getAttachedShop();
          if (Objects.requireNonNull(nextTo).getPrice() > shop.getPrice()) {
            // The one next to it must always be a
            // buying shop.
            p.sendMessage(MsgUtil.getMessage("buying-more-than-selling", p));
          }
        }
      }

      private void actionSell(
          @NotNull Player p,
          @NotNull Economy eco,
          @NotNull HashMap<UUID, Info> actions2,
          @NotNull Info info,
          @NotNull String message,
          @NotNull Shop shop, int amount) {

        int stock = shop.getRemainingStock();
        if (stock == -1) {
          stock = 10000;
        }
        if (stock < amount) {
          p.sendMessage(MsgUtil.getMessage("shop-stock-too-low", p, "" + stock,
              Util.getItemStackName(shop.getItem())));
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
        double tax = QuickShop.instance().getConfig().getDouble("tax");
        double total = amount * shop.getPrice();
        if (QuickShop.getPermissionManager().hasPermission(p, "quickshop.tax")) {
          tax = 0;
          Util.debugLog("Disable the Tax for player " + p.getName()
          + " cause they have permission quickshop.tax");
        }
        if (tax < 0) {
          tax = 0; // Tax was disabled.
        }
        if (shop.getModerator().isModerator(p.getUniqueId())) {
          tax = 0; // Is staff or owner, so we won't will take them tax
        }

        boolean successA = eco.withdraw(p.getUniqueId(), total); // Withdraw owner's money
        if (!successA) {
          p.sendMessage(
              MsgUtil.getMessage("you-cant-afford-to-buy", p, Objects.requireNonNull(format(total)),
                  Objects.requireNonNull(format(eco.getBalance(p.getUniqueId())))));
          return;
        }
        boolean shouldPayOwner = !shop.isUnlimited()
            || (QuickShop.instance().getConfig().getBoolean("shop.pay-unlimited-shop-owners") && shop.isUnlimited());
        if (shouldPayOwner) {
          double depositMoney = total * (1 - tax);
          boolean successB = eco.deposit(shop.getOwner(), depositMoney);
          if (!successB) {
            QuickShop.instance().getLogger().warning(
                "Failed to deposit the money for player " + Bukkit.getOfflinePlayer(shop.getOwner()));
            /* Rollback the trade */
            if (!eco.deposit(p.getUniqueId(), depositMoney)) {
              QuickShop.instance().getLogger().warning("Failed to rollback the purchase actions for player "
                  + Bukkit.getOfflinePlayer(shop.getOwner()).getName());
            }
            p.sendMessage(MsgUtil.getMessage("purchase-failed", p));
            return;
          }
        }

        String msg;
        // Notify the shop owner
        if (QuickShop.instance().getConfig().getBoolean("show-tax")) {
          msg = MsgUtil.getMessage("player-bought-from-your-store-tax", p, p.getName(), "" + amount,
              "##########" + Util.serialize(shop.getItem()) + "##########", Util.format((tax * total)));
        } else {
          msg = MsgUtil.getMessage("player-bought-from-your-store", p, p.getName(), "" + amount,
              "##########" + Util.serialize(shop.getItem()) + "##########");
        }
        // Transfers the item from A to B
        if (stock == amount) {
          msg += "\n" + MsgUtil.getMessage("shop-out-of-stock", p, "" + shop.getLocation().getBlockX(),
              "" + shop.getLocation().getBlockY(), "" + shop.getLocation().getBlockZ(),
              Util.getItemStackName(shop.getItem()));
        }

        MsgUtil.send(shop.getOwner(), msg, shop.isUnlimited());
        shop.sell(p, amount);
        MsgUtil.sendPurchaseSuccess(p, shop, amount);
        ShopSuccessPurchaseEvent se = new ShopSuccessPurchaseEvent(shop, p, amount, total, tax);
        Bukkit.getPluginManager().callEvent(se);
      }

      private void actionTrade(
          @NotNull Player p,
          @NotNull HashMap<UUID, Info> actions,
          @NotNull Info info,
          @NotNull String message) {

        if (QuickShop.instance().getEconomy() == null) {
          p.sendMessage("Error: Economy system not loaded, type /qs main command to get details.");
          return;
        }

        if (!QuickShop.instance().getIntegrationHelper().callIntegrationsCanTrade(p, info.getLocation())) {
          Util.debugLog("Cancel by integrations.");
          return;
        }

        Economy eco = QuickShop.instance().getEconomy();

        // Shop gone
        // Get the shop they interacted with
        ShopViewer shopOp = getShop(info.getLocation());
        // It's not valid anymore
        if (!shopOp.isPresent() || !Util.canBeShop(info.getLocation().getBlock())) {
          p.sendMessage(MsgUtil.getMessage("chest-was-removed", p));
          return;
        }

        // Shop changed
        Shop shop = shopOp.get();
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
          if (message.equalsIgnoreCase(
              QuickShop.instance().getConfig().getString("shop.word-for-trade-all-items", "all")))
            amount = -1;

          p.sendMessage(MsgUtil.getMessage("not-a-integer", p, message));
          Util.debugLog("Receive the chat " + message + " and it format failed: " + e.getMessage());
          return;
        }

        if (shop.isBuying()) {
          actionBuy(p, eco, actions, info, message, shop, amount);
        } else if (shop.isSelling()) {
          actionSell(p, eco, actions, info, message, shop, amount);
        } else {
          p.sendMessage(MsgUtil.getMessage("shop-purchase-cancelled", p));
          QuickShop.instance().getLogger().warning("Shop data broken? Loc:" + shop.getLocation());
        }
      }

      /**
       * Adds a shop to the world. Does NOT require the chunk or world to be loaded Call shop.onLoad by
       * yourself
       *
       * @param world The name of the world
       * @param shop The shop to add
       */
      public void addShop(@NotNull String world, @NotNull Shop shop) {
        HashMap<ShopChunk, HashMap<Location, Shop>> inWorld =
            this.getShops().computeIfAbsent(world, k -> new HashMap<>(3));
        // There's no world storage yet. We need to create that hashmap.
        // Put it in the data universe
        // Calculate the chunks coordinates. These are 1,2,3 for each chunk, NOT
        // location rounded to the nearest 16.
        int x = (int) Math.floor((shop.getLocation().getBlockX()) / 16.0);
        int z = (int) Math.floor((shop.getLocation().getBlockZ()) / 16.0);
        // Get the chunk set from the world info
        ShopChunk shopChunk = new ShopChunk(world, x, z);
        HashMap<Location, Shop> inChunk = inWorld.computeIfAbsent(shopChunk, k -> new HashMap<>(1));
        // That chunk data hasn't been created yet - Create it!
        // Put it in the world
        // Put the shop in its location in the chunk list.
        inChunk.put(shop.getLocation(), shop);
        // shop.onLoad();

      }

      /**
       * Checks other plugins to make sure they can use the chest they're making a shop.
       *
       * @param p The player to check
       * @param b The block to check
       * @param bf The blockface to check
       * @return True if they're allowed to place a shop there.
       */
      public boolean canBuildShop(@NotNull Player p, @NotNull Block b, @NotNull BlockFace bf) {
        try {
          QuickShop.instance().getCompatibilityTool().toggleProtectionListeners(false, p);

          if (QuickShop.instance().isLimit()) {
            int owned = 0;
            if (!QuickShop.instance().getConfig().getBoolean("limits.old-algorithm")) {
              for (HashMap<ShopChunk, HashMap<Location, Shop>> shopmap : getShops().values()) {
                for (HashMap<Location, Shop> shopLocs : shopmap.values()) {
                  for (Shop shop : shopLocs.values()) {
                    if (shop.getOwner().equals(p.getUniqueId()) && !shop.isUnlimited()) {
                      owned++;
                    }
                  }
                }
              }
            } else {
              Iterator<Shop> it = getShopIterator();
              while (it.hasNext()) {
                if (it.next().getOwner().equals(p.getUniqueId())) {
                  owned++;
                }
              }
            }

            int max = QuickShop.instance().getShopLimit(p);
            if (owned + 1 > max) {
              p.sendMessage(MsgUtil.getMessage("reached-maximum-can-create", p, String.valueOf(owned),
                  String.valueOf(max)));
              return false;
            }
          }
          if (!QuickShop.instance().getPermissionChecker().canBuild(p, b)) {
            Util.debugLog("PermissionChecker canceled shop creation");
            return false;
          }
          ShopPreCreateEvent spce = new ShopPreCreateEvent(p, b.getLocation());
          Bukkit.getPluginManager().callEvent(spce);
          if (Util.fireCancellableEvent(spce)) {
            return false;
          }
        } finally {
          QuickShop.instance().getCompatibilityTool().toggleProtectionListeners(true, p);
        }

        return true;
      }

      /**
       * Removes all shops from memory and the world. Does not delete them from the database. Call this
       * on plugin disable ONLY.
       */
      public void clear() {
        if (BaseConfig.displayItems) {
          for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
              HashMap<Location, Shop> inChunk = this.getShops(chunk);
              if (inChunk == null || inChunk.isEmpty()) {
                continue;
              }
              for (Shop shop : inChunk.values()) {
                shop.onUnload();
              }
            }
          }
        }
        this.actions.clear();
        this.shops.clear();
      }

      /**
       * Create a shop use Shop and Info object.
       *
       * @param shop The shop object
       * @param info The info object
       */
      public void createShop(@NotNull Shop shop, @NotNull Info info) {
        Player player = Bukkit.getPlayer(shop.getOwner());
        if (player == null) {
          throw new IllegalStateException("The owner creating the shop is offline or not exist");
        }
        ShopCreateEvent ssShopCreateEvent = new ShopCreateEvent(shop, player);
        if (Util.fireCancellableEvent(ssShopCreateEvent)) {
          Util.debugLog("Cancelled by plugin");
          return;
        }
        Location loc = shop.getLocation();
        try {
          // Write it to the database
          QuickShop.instance().getDatabaseHelper().createShop(ShopModerator.serialize(shop.getModerator()),
              shop.getPrice(), shop.getItem(), (shop.isUnlimited() ? 1 : 0), shop.getShopType().toID(),
              Objects.requireNonNull(loc.getWorld()).getName(), loc.getBlockX(), loc.getBlockY(),
              loc.getBlockZ());
          // Add it to the world
          addShop(loc.getWorld().getName(), shop);
        } catch (SQLException error) {
          QuickShop.instance().getLogger().warning("SQLException detected, trying to auto fix the database...");
          boolean backupSuccess = Util.backupDatabase();
          try {
            if (backupSuccess) {
              QuickShop.instance().getDatabaseHelper().deleteShop(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                  loc.getWorld().getName());
            } else {
              QuickShop.instance().getLogger()
              .warning("Failed to backup the database, all changes will revert after a reboot.");
            }
          } catch (SQLException error2) {
            // Failed removing
            QuickShop.instance().getLogger()
            .warning("Failed to autofix the database, all changes will revert after a reboot.");
            error2.printStackTrace();
          }
          error.printStackTrace();
        }
        // Create sign
        if (info.getSignBlock() != null && QuickShop.instance().getConfig().getBoolean("shop.auto-sign")) {
          if (!Util.isAir(info.getSignBlock().getType())) {
            Util.debugLog("Sign cannot placed cause no enough space(Not air block)");
            return;
          }
          boolean isWaterLogged = false;
          if (info.getSignBlock().getType() == Material.WATER) {
            isWaterLogged = true;
          }

          info.getSignBlock().setType(Util.getSignMaterial());
          BlockState bs = info.getSignBlock().getState();
          if (isWaterLogged) {
            if (bs.getBlockData() instanceof Waterlogged) {
              Waterlogged waterable = (Waterlogged) bs.getBlockData();
              waterable.setWaterlogged(true); // Looks like sign directly put in water
            }
          }
          if (bs.getBlockData() instanceof WallSign) {
            org.bukkit.block.data.type.WallSign signBlockDataType =
                (org.bukkit.block.data.type.WallSign) bs.getBlockData();
            BlockFace bf = info.getLocation().getBlock().getFace(info.getSignBlock());
            if (bf != null) {
              signBlockDataType.setFacing(bf);
              bs.setBlockData(signBlockDataType);
            }
          } else {
            QuickShop.instance().getLogger().warning("Sign material " + bs.getType().name()
                + " not a WallSign, make sure you using correct sign material.");
          }
          bs.update(true);
          shop.setSignText();
        }
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
      
      public ShopViewer getShop(@NotNull Block block) {
        return getShop(block);
      }

      /**
       * Gets a shop in a specific location
       *
       * @param loc The location to get the shop from
       * @return The shop at that location
       */
      public ShopViewer getShop(@NotNull Location loc) {
        HashMap<Location, Shop> inChunk = getShops(loc.getChunk());
        if (inChunk == null) {
          return ShopViewer.empty();
        }
        loc = loc.clone();
        // Fix double chest XYZ issue
        loc.setX(loc.getBlockX());
        loc.setY(loc.getBlockY());
        loc.setZ(loc.getBlockZ());
        // We can do this because WorldListener updates the world reference so
        // the world in loc is the same as world in inChunk.get(loc)
        return ShopViewer.of(inChunk.get(loc));
      }
      
      public void accept(@NotNull Location loc, @NotNull Consumer<Shop> consumer) {
        HashMap<Location, Shop> inChunk = getShops(loc.getChunk());
        if (inChunk != null) {
          loc = loc.clone();
          // Fix double chest XYZ issue
          loc.setX(loc.getBlockX());
          loc.setY(loc.getBlockY());
          loc.setZ(loc.getBlockZ());
          // We can do this because WorldListener updates the world reference so
          // the world in loc is the same as world in inChunk.get(loc)
          Shop shop = inChunk.get(loc);
          if (shop != null)
            consumer.accept(shop);
        }
      }
      
      /**
       * Gets a shop in a specific location Include the attached shop, e.g DoubleChest shop.
       *
       * @param loc The location to get the shop from
       * @return The shop at that location
       */
      public ShopViewer getShopIncludeAttached(@Nullable Location loc) {
        if (loc == null) {
          Util.debugLog("Location is null.");
          return null;
        }

        if (BaseConfig.useFastShopSearchAlgorithm) {
          return getShopIncludeAttached_Fast(loc);
        } else {
          return getShopIncludeAttached_Classic(loc);
        }
      }

      @Nullable
      public ShopViewer getShopIncludeAttached_Classic(@NotNull Location loc) {
        @Nullable Shop shop;
        // Get location's chunk all shops
        @Nullable HashMap<Location, Shop> inChunk = getShops(loc.getChunk());
        // Found some shops in this chunk.
        if (inChunk != null) {
          shop = inChunk.get(loc);
          if (shop != null) {
            // Okay, shop was founded.
            return ShopViewer.of(shop);
          }
          // Ooops, not founded that shop in this chunk.
        }
        @Nullable
        Block secondHalfShop = Util.getSecondHalf(loc.getBlock());
        if (secondHalfShop != null) {
          inChunk = getShops(secondHalfShop.getChunk());
          if (inChunk != null) {
            shop = inChunk.get(secondHalfShop.getLocation());
            if (shop != null) {
              // Okay, shop was founded.
              return ShopViewer.of(shop);
            }
            // Oooops, no any shops matched.
          }
        }
        // If that chunk nothing we founded, we should check it is attached.
        @Nullable
        Block attachedBlock = Util.getAttached(loc.getBlock());
        // Check is attached on some block.
        if (attachedBlock == null) {
          // Nope
          Util.debugLog("No attached block.");
          return ShopViewer.empty();
        } else {
          // Okay we know it on some blocks.
          // We need set new location and chunk.
          inChunk = getShops(attachedBlock.getChunk());
          // Found some shops in this chunk
          if (inChunk != null) {
            shop = inChunk.get(attachedBlock.getLocation());
            if (shop != null) {
              // Okay, shop was founded.
              return ShopViewer.empty();
            }
            // Oooops, no any shops matched.
          }
        }

        Util.debugLog("Not found shops use the attached util.");

        return ShopViewer.empty();
      }

      private ShopViewer getShopIncludeAttached_Fast(@NotNull Location loc) {
        ShopViewer shop = getShop(loc);
        if (shop.isPresent()) {
          return shop;
        }
        @Nullable Block attachedBlock = Util.getSecondHalf(loc.getBlock());
        if (attachedBlock != null) {
          return getShop(attachedBlock.getLocation());
        } else {
          return ShopViewer.empty();
        }
      }

      /**
       * Returns a hashmap of Chunk - Shop
       *
       * @param world The name of the world (case sensitive) to get the list of shops from
       * @return a hashmap of Chunk - Shop
       */
      public @Nullable HashMap<ShopChunk, HashMap<Location, Shop>> getShops(@NotNull String world) {
        return this.shops.get(world);
      }

      /**
       * Returns a hashmap of Shops
       *
       * @param c The chunk to search. Referencing doesn't matter, only coordinates and world are used.
       * @return Shops
       */
      public @Nullable HashMap<Location, Shop> getShops(@NotNull Chunk c) {
        // long start = System.nanoTime();
        return getShops(c.getWorld().getName(), c.getX(), c.getZ());
        // long end = System.nanoTime();
        // QuickShop.instance().getLogger().log(Level.WARNING, "Chunk lookup in " + ((end - start)/1000000.0) +
        // "ms.");
      }

      public @Nullable HashMap<Location, Shop> getShops(@NotNull String world, int chunkX, int chunkZ) {
        HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops(world);
        if (inWorld == null) {
          return null;
        }
        ShopChunk shopChunk = new ShopChunk(world, chunkX, chunkZ);
        return inWorld.get(shopChunk);
      }

      public void handleChat(@NotNull Player p, @NotNull String msg) {
        handleChat(p, msg, false);
      }

      public void handleChat(@NotNull Player p, @NotNull String msg, boolean bypassProtectionChecks) {
        final String message = ChatColor.stripColor(msg);
        // Use from the main thread, because Bukkit hates life
        Bukkit.getScheduler().runTask(QuickShop.instance(), () -> {
          HashMap<UUID, Info> actions = getActions();
          // They wanted to do something.
          Info info = actions.remove(p.getUniqueId());
          if (info == null) {
            return; // multithreaded means this can happen
          }

          if (info.getLocation().getWorld() != p.getLocation().getWorld()
              || info.getLocation().distanceSquared(p.getLocation()) > 25) {
            p.sendMessage(MsgUtil.getMessage("not-looking-at-shop", p));
            return;
          }
          if (info.getAction() == ShopAction.CREATE) {
            actionCreate(p, actions, info, message, bypassProtectionChecks);
          }
          if (info.getAction() == ShopAction.BUY) {
            actionTrade(p, actions, info, message);
          }
        });
      }

      /**
       * Loads the given shop into storage. This method is used for loading data from the database. Do
       * not use this method to create a shop.
       *
       * @param world The world the shop is in
       * @param shop The shop to load
       */
      public void loadShop(@NotNull String world, @NotNull Shop shop) {
        this.addShop(world, shop);
      }

      /**
       * Removes a shop from the world. Does NOT remove it from the database. * REQUIRES * the world to
       * be loaded Call shop.onUnload by your self.
       *
       * @param shop The shop to remove
       */
      public void unloadShop(@NotNull Shop shop) {
        // shop.onUnload();
        Location loc = shop.getLocation();
        String world = Objects.requireNonNull(loc.getWorld()).getName();
        HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops().get(world);
        int x = (int) Math.floor((shop.getLocation().getBlockX()) / 16.0);
        int z = (int) Math.floor((shop.getLocation().getBlockZ()) / 16.0);
        ShopChunk shopChunk = new ShopChunk(world, x, z);
        HashMap<Location, Shop> inChunk = inWorld.get(shopChunk);
        if (inChunk == null) {
          return;
        }
        inChunk.remove(loc);
        // shop.onUnload();
      }

      /** @return Returns the HashMap. Info contains what their last question etc was. */
      public HashMap<UUID, Info> getActions() {
        return this.actions;
      }

      /**
       * Returns a new shop iterator object, allowing iteration over shops easily, instead of sorting
       * through a 3D hashmap.
       *
       * @return a new shop iterator object.
       */
      public Iterator<Shop> getShopIterator() {
        return new ShopIterator();
      }

      /**
       * Returns all shops in the whole database, include unloaded.
       *
       * <p>
       * Make sure you have caching this, because this need a while to get all shops
       *
       * @return All shop in the database
       */
      public Collection<Shop> getAllShops() {
        // noinspection unchecked
        HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>> worldsMap =
            (HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>>) getShops().clone();
        Collection<Shop> shops = new ArrayList<>();
        for (HashMap<ShopChunk, HashMap<Location, Shop>> shopMapData : worldsMap.values()) {
          for (HashMap<Location, Shop> shopData : shopMapData.values()) {
            shops.addAll(shopData.values());
          }
        }
        return shops;
      }

      /**
       * Returns a hashmap of World - Chunk - Shop
       *
       * @return a hashmap of World - Chunk - Shop
       */
      @NotNull
      public HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>> getShops() {
        return this.shops;
      }

      /**
       * Get all loaded shops.
       *
       * @return All loaded shops.
       */
      @NotNull
      public Set<Shop> getLoadedShops() {
        return this.loadedShops;
      }

      /**
       * Get a players all shops.
       *
       * @param playerUUID The player's uuid.
       * @return The list have this player's all shops.
       */
      public @NotNull List<Shop> getPlayerAllShops(@NotNull UUID playerUUID) {
        return getAllShops().stream().filter(shop -> shop.getOwner().equals(playerUUID))
            .collect(Collectors.toList());
      }

      /**
       * Get the all shops in the world.
       *
       * @param world The world you want get the shops.
       * @return The list have this world all shops
       */
      public @NotNull List<Shop> getShopsInWorld(@NotNull World world) {
        return getAllShops().stream()
            .filter(shop -> Objects.equals(shop.getLocation().getWorld(), world))
            .collect(Collectors.toList());
      }

      public class ShopIterator implements Iterator<Shop> {
        private Iterator<HashMap<Location, Shop>> chunks;
        private Iterator<Shop> shops;
        private Iterator<HashMap<ShopChunk, HashMap<Location, Shop>>> worlds;

        public ShopIterator() {
          // noinspection unchecked
          HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>> worldsMap =
              (HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>>) getShops().clone();
          worlds = worldsMap.values().iterator();
        }

        /** Returns true if there is still more shops to iterate over. */
        @Override
        public boolean hasNext() {
          if (shops == null || !shops.hasNext()) {
            if (chunks == null || !chunks.hasNext()) {
              if (!worlds.hasNext()) {
                return false;
              } else {
                chunks = worlds.next().values().iterator();
                return hasNext();
              }
            } else {
              shops = chunks.next().values().iterator();
              return hasNext();
            }
          }
          return true;
        }

        /** Fetches the next shop. Throws NoSuchElementException if there are no more shops. */
        @Override
        public @NotNull Shop next() {
          if (shops == null || !shops.hasNext()) {
            if (chunks == null || !chunks.hasNext()) {
              if (!worlds.hasNext()) {
                throw new NoSuchElementException("No more shops to iterate over!");
              }
              chunks = worlds.next().values().iterator();
            }
            shops = chunks.next().values().iterator();
          }
          if (!shops.hasNext()) {
            return this.next(); // Skip to the next one (Empty iterator?)
          }
          return shops.next();
        }
      }
}
