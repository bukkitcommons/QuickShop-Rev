package org.maxgamer.quickshop.integration.impl;

import com.google.common.collect.Lists;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.perms.PermissibleAction;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.Configuration;
import org.maxgamer.quickshop.configuration.Node;
import org.maxgamer.quickshop.integration.IntegratedPlugin;

@Configuration("integrations.yml")
public class FactionsIntegration implements IntegratedPlugin {
  @Node(value = "integrations.factions.create.flags")
  public static List<String> createFlags = Lists.newArrayList();
  
  @Node(value = "integrations.factions.create.flags")
  public static List<String> tradeFlags = Lists.newArrayList();
  
  @Node(value = "integrations.factions.enabled")
  public static boolean enabled;
  
  @Node(value = "integrations.factions.create.require-open")
  public static boolean createRequireOpen;
  @Node(value = "integrations.factions.create.require-normal")
  public static boolean createRequireNormal;
  @Node(value = "integrations.factions.create.require-wilderness")
  public static boolean createRequireWilderness;
  @Node(value = "integrations.factions.create.require-peaceful")
  public static boolean createRequirePeaceful;
  @Node(value = "integrations.factions.create.require-permanent")
  public static boolean createRequirePermanent;
  @Node(value = "integrations.factions.create.require-safezone")
  public static boolean createRequireSafeZone;
  @Node(value = "integrations.factions.create.require-own")
  public static boolean createRequireOwn;
  @Node(value = "integrations.factions.create.require-warzone")
  public static boolean createRequireWarZone;
  @Node(value = "integrations.factions.trade.require-open")
  public static boolean tradeRequireOpen;
  @Node(value = "integrations.factions.trade.require-normal")
  public static boolean tradeRequireNormal;
  @Node(value = "integrations.factions.trade.require-wilderness")
  public static boolean tradeRequireWilderness;
  @Node(value = "integrations.factions.trade.require-peaceful")
  public static boolean tradeRequirePeaceful;
  @Node(value = "integrations.factions.trade.require-permanent")
  public static boolean tradeRequirePermanent;
  @Node(value = "integrations.factions.trade.require-safezone")
  public static boolean tradeRequireSafeZone;
  @Node(value = "integrations.factions.trade.require-own")
  public static boolean tradeRequireOwn;
  @Node(value = "integrations.factions.trade.require-warzone")
  public static boolean tradeRequireWarZone;

  @Override
  public @NotNull String getName() {
    return "Factions";
  }

  @Override
  public boolean canCreateShopHere(@NotNull Player player, @NotNull Location location) {
    Faction faction = Board.getInstance().getFactionAt(new FLocation(location));
    if (faction == null) {
      return true;
    }
    if (createRequireOpen && !faction.getOpen()) {
      return false;
    }
    if (createRequireSafeZone && !faction.isSafeZone()) {
      return false;
    }
    if (createRequirePermanent && !faction.isPermanent()) {
      return false;
    }
    if (createRequirePeaceful && !faction.isPeaceful()) {
      return false;
    }
    if (createRequireWilderness && !faction.isWilderness()) {
      return false;
    }
    if (createRequireOpen && !faction.getOpen()) {
      return false;
    }
    if (createRequireWarZone && !faction.isWarZone()) {
      return false;
    }
    if (createRequireNormal && !faction.isNormal()) {
      return false;
    }
    if (createRequireOwn
        && !faction.getOwnerList(new FLocation(location)).contains(player.getName())) {
      return false;
    }
    for (String flag : createFlags) {
      if (!faction.hasAccess(FPlayers.getInstance().getByPlayer(player),
          PermissibleAction.fromString(flag))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean canTradeShopHere(@NotNull Player player, @NotNull Location location) {

    Faction faction = Board.getInstance().getFactionAt(new FLocation(location));
    if (faction == null) {
      return true;
    }
    if (tradeRequireOpen && !faction.getOpen()) {
      return false;
    }
    if (tradeRequireSafeZone && !faction.isSafeZone()) {
      return false;
    }
    if (tradeRequirePermanent && !faction.isPermanent()) {
      return false;
    }
    if (tradeRequirePeaceful && !faction.isPeaceful()) {
      return false;
    }
    if (tradeRequireWilderness && !faction.isWilderness()) {
      return false;
    }
    if (tradeRequireOpen && !faction.getOpen()) {
      return false;
    }
    if (tradeRequireWarZone && !faction.isWarZone()) {
      return false;
    }
    if (tradeRequireNormal && !faction.isNormal()) {
      return false;
    }
    if (tradeRequireOwn
        && !faction.getOwnerList(new FLocation(location)).contains(player.getName())) {
      return false;
    }
    for (String flag : tradeFlags) {
      if (!faction.hasAccess(FPlayers.getInstance().getByPlayer(player),
          PermissibleAction.fromString(flag))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void load() {}

  @Override
  public void unload() {}
}
