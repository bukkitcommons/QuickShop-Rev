package org.maxgamer.quickshop.integration;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.google.common.collect.Lists;
import cc.bukkit.shop.configuration.Configuration;
import cc.bukkit.shop.configuration.Node;
import cc.bukkit.shop.integration.IntegratedPlugin;

@Configuration("configs/integrations.yml")
public class ResidenceIntegration implements IntegratedPlugin {
  @Node("integration.residence.create")
  public static List<String> createLimits = Lists.newArrayList();
  @Node("integration.residence.trade")
  public static List<String> tradeLimits = Lists.newArrayList();

  @Override
  public @NotNull String getName() {
    return "Residence";
  }

  @Override
  public boolean canCreateShopHere(@NotNull Player player, @NotNull Location location) {
    ClaimedResidence residence = Residence.getInstance().getResidenceManager().getByLoc(location);

    for (String limit : this.createLimits) {
      if ("FLAG".equalsIgnoreCase(limit)) {
        if (residence == null) {
          // Check world permission
          if (!Residence.getInstance().getWorldFlags().getPerms(location.getWorld().getName())
              .playerHas(player, Flags.getFlag("quickshop-create"), false)) {
            return false;
          }
        } else {
          if (!residence.getPermissions().playerHas(player, Flags.getFlag("quickshop-create"),
              false)) {
            return false;
          }
        }
      }
      // Not flag
      if (residence == null) {
        if (!Residence.getInstance().getWorldFlags().getPerms(location.getWorld().getName())
            .playerHas(player, Flags.getFlag(limit), false)) {
          return false;
        }
      } else {
        if (!residence.getPermissions().playerHas(player, Flags.getFlag(limit), false)) {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public boolean canTradeShopHere(@NotNull Player player, @NotNull Location location) {
    ClaimedResidence residence = Residence.getInstance().getResidenceManager().getByLoc(location);

    for (String limit : this.tradeLimits) {
      if ("FLAG".equalsIgnoreCase(limit)) {
        if (residence == null) {
          // Check world permission
          if (!Residence.getInstance().getWorldFlags().getPerms(location.getWorld().getName())
              .playerHas(player, Flags.getFlag("quickshop-trade"), false)) {
            return false;
          }
        } else {
          if (!residence.getPermissions().playerHas(player, Flags.getFlag("quickshop-trade"),
              true)) {
            return false;
          }
        }
      }
      // Not flag
      if (residence == null) {
        if (!Residence.getInstance().getWorldFlags().getPerms(location.getWorld().getName())
            .playerHas(player, Flags.getFlag(limit), false)) {
          return false;
        }
      } else {
        if (!residence.getPermissions().playerHas(player, Flags.getFlag(limit), false)) {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public void load() {
    FlagPermissions.addFlag("quickshop.create");
    FlagPermissions.addFlag("quickshop.trade");
  }

  @Override
  public void unload() {}
}
