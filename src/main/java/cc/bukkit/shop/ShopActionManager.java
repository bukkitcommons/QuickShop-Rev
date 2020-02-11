package cc.bukkit.shop;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.data.ShopActionData;
import cc.bukkit.shop.data.ShopCreator;
import cc.bukkit.shop.data.ShopSnapshot;

public interface ShopActionManager {
  boolean hasAction(@NotNull UUID player);
  
  void setAction(@NotNull UUID player, @NotNull ShopActionData data);
  
  void removeAction(@NotNull UUID player);

  boolean actionBuy(
      @NotNull Player p,
      @NotNull String message,
      @NotNull ContainerShop shop, int amount,
      @NotNull ShopSnapshot info);

  void actionCreate(
      @NotNull Player p,
      @NotNull ShopCreator info,
      @NotNull String message,
      boolean bypassProtectionChecks);

  void actionSell(
      @NotNull Player p,
      @NotNull String message,
      @NotNull ContainerShop shop, int amount,
      @NotNull ShopSnapshot info);

  void actionTrade(
      @NotNull Player p,
      @NotNull ShopSnapshot info,
      @NotNull String message);

  void handleChat(@NotNull Player p, @NotNull String msg, boolean sync);

  void handleChat(@NotNull Player p, @NotNull String msg, boolean bypassProtectionChecks, boolean sync);

  void clear();
}
