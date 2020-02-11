package org.maxgamer.quickshop.integration;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import cc.bukkit.shop.configuration.Configuration;
import cc.bukkit.shop.configuration.ConfigurationData;
import cc.bukkit.shop.integration.IntegrateStage;
import cc.bukkit.shop.integration.IntegratedPlugin;
import cc.bukkit.shop.integration.IntegrationStage;

@Configuration("configs/integrations.yml")
@IntegrationStage(loadStage = IntegrateStage.POST_LOAD)
public class WorldGuardIntegration implements IntegratedPlugin {
  private List<WorldGuardFlags> createFlags;
  private List<WorldGuardFlags> tradeFlags;
  private StateFlag createFlag = new StateFlag("quickshop-create", false);
  private StateFlag tradeFlag = new StateFlag("quickshop-trade", true);

  public WorldGuardIntegration() {
    ConfigurationData data =
        QuickShop.instance().getConfigurationManager().load(WorldGuardIntegration.class);
    
    createFlags = WorldGuardFlags
        .deserialize(data.conf().getStringList("integration.worldguard.create"));
    tradeFlags = WorldGuardFlags
        .deserialize(data.conf().getStringList("integration.worldguard.trade"));
  }

  @Override
  public void load() {
    FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
    try {
      // create a flag with the name "my-custom-flag", defaulting to true
      registry.register(this.createFlag);
      registry.register(this.tradeFlag);
      ShopLogger.instance().info(ChatColor.GREEN + getName() + " flags register successfully.");
      Util.debug("Success register " + getName() + " flags.");
    } catch (FlagConflictException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void unload() {}

  @Override
  public @NotNull String getName() {
    return "WorldGuard";
  }

  @Override
  public boolean canCreateShopHere(@NotNull Player player, @NotNull Location location) {
    LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(location);
    boolean canBypass = WorldGuard.getInstance().getPlatform().getSessionManager()
        .hasBypass(localPlayer, BukkitAdapter.adapt(location.getWorld()));
    if (canBypass) {
      Util.debug("Player " + player.getName()
          + " bypassing the protection checks, because player have bypass permission in WorldGuard");
      return true;
    }
    RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    RegionQuery query = container.createQuery();
    for (WorldGuardFlags flag : createFlags) {
      switch (flag) {
        case BUILD:
          if (query.queryState(wgLoc, localPlayer, Flags.BUILD) == StateFlag.State.DENY) {
            return false;
          }
          break;
        case FLAG:
          if (query.queryState(wgLoc, localPlayer, this.createFlag) == StateFlag.State.DENY) {
            return false;
          }
          break;
        case CHEST_ACCESS:
          if (query.queryState(wgLoc, localPlayer, Flags.CHEST_ACCESS) == StateFlag.State.DENY) {
            return false;
          }
          break;
        case INTERACT:
          if (query.queryState(wgLoc, localPlayer, Flags.INTERACT) == StateFlag.State.DENY) {
            return false;
          }
          break;
      }
    }
    return true;
  }

  @Override
  public boolean canTradeShopHere(@NotNull Player player, @NotNull Location location) {
    LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(location);
    boolean canBypass = WorldGuard.getInstance().getPlatform().getSessionManager()
        .hasBypass(localPlayer, BukkitAdapter.adapt(location.getWorld()));
    if (canBypass) {
      Util.debug("Player " + player.getName()
          + " bypassing the protection checks, because player have bypass permission in WorldGuard");
      return true;
    }
    RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    RegionQuery query = container.createQuery();
    for (WorldGuardFlags flag : tradeFlags) {
      switch (flag) {
        case BUILD:
          if (!query.testState(wgLoc, localPlayer, Flags.BUILD)) {
            return false;
          }

          break;
        case FLAG:
          if (!query.testState(wgLoc, localPlayer, this.tradeFlag)) {
            return false;
          }
          break;
        case CHEST_ACCESS:
          if (!query.testState(wgLoc, localPlayer, Flags.CHEST_ACCESS)) {
            return false;
          }
          break;
        case INTERACT:
          if (!query.testState(wgLoc, localPlayer, Flags.INTERACT)) {
            return false;
          }
          break;
      }
    }
    return true;
  }
}
