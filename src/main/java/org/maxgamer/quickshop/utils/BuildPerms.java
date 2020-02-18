package org.maxgamer.quickshop.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.configuration.BaseConfig;
import cc.bukkit.shop.event.ProtectionCheckStatus;
import cc.bukkit.shop.event.ShopProtectionCheckEvent;
import cc.bukkit.shop.misc.ShopLocation;

public class BuildPerms {
  /**
   * Check player can build in target location
   *
   * @param player Target player
   * @param shopLocation Target location
   * @return Success
   */
  public boolean canBuild(@NotNull Player player, @NotNull ShopLocation shopLocation) {
    return canBuild(player, shopLocation.block());
  }

  /**
   * Check player can build in target block
   *
   * @param player Target player
   * @param block Target block
   * @return Success
   */
  public boolean canBuild(@NotNull Player player, @NotNull Block block) {
    if (!BaseConfig.enableProtection)
      return true;
    
    BlockPlaceEvent facadePlace;
    Material signType = Material.getMaterial("OAK_WALL_SIGN");
    signType = signType == null ? Material.getMaterial("WALL_SIGN") : signType;
    BlockState air = block.getState();
               air.setType(Material.AIR);
    
    facadePlace = new BlockPlaceEvent(
        block, air, block, new ItemStack(signType), player, true, EquipmentSlot.HAND);
    // Call for event for protection check start
    
    Bukkit.getPluginManager().callEvent(new ShopProtectionCheckEvent(block.getLocation(), player,
        ProtectionCheckStatus.BEGIN, facadePlace));
    Bukkit.getPluginManager().callEvent(facadePlace);
    // Call for event for protection check end
    Bukkit.getPluginManager().callEvent(new ShopProtectionCheckEvent(block.getLocation(), player,
        ProtectionCheckStatus.END, facadePlace));
    
    return !facadePlace.isCancelled();
  }
}
