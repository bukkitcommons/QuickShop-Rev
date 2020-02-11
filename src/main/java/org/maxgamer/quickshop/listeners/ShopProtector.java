package org.maxgamer.quickshop.listeners;

import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.shop.QuickShopLoader;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.viewer.ShopViewer;

public class ShopProtector implements Listener {
  private static void handleProtection(
      @NotNull Location location,
      @NotNull Runnable protection) {
    handleProtection(location, true, protection, shop -> {});
  }

  private static void handleProtection(
      @NotNull Location location,
      @NotNull boolean predicate,
      @NotNull Runnable protection,
      @NotNull Consumer<ContainerShop> orElse) {

    Shop.getManager()
    .getLoadedShopFrom(location)

    .ifPresent(shop -> {
      if (predicate)
        protection.run();
      else
        orElse.accept(shop);
    });
  }

  /*
   * Basic protections
   */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockExplode(BlockExplodeEvent event) {
    for (Block block : event.blockList())
      handleProtection(block.getLocation(), BaseConfig.explosionProtection,
          () -> event.setCancelled(true),
          shop -> QuickShopLoader.instance().delete(shop));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    for (Block block : event.blockList())
      handleProtection(block.getLocation(), BaseConfig.explosionProtection,
          () -> event.setCancelled(true),
          shop -> QuickShopLoader.instance().delete(shop));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onMobChangeBlock(EntityChangeBlockEvent event) {
    handleProtection(event.getBlock().getLocation(), () -> event.setCancelled(true));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockSpread(BlockSpreadEvent event) {
    handleProtection(event.getNewState().getBlock().getLocation(), () -> event.setCancelled(true));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH) // FIXME
  public void onInventoryMove(InventoryMoveItemEvent event) {
    final Location loc = event.getSource().getLocation();

    if (loc != null && !Util.isBlocklisted(loc.getBlock().getType())) // FIXME maybe dupe
      handleProtection(event.getSource().getLocation(), () -> event.setCancelled(true));
  }

  /*
   * Enhanced protections
   */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockRedstoneChange(BlockRedstoneEvent event) { // FIXME configure
    if (BaseConfig.enhancedShopProtection)
      handleProtection(event.getBlock().getLocation(), () -> event.setNewCurrent(event.getOldCurrent()));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onStructureGrow(StructureGrowEvent event) {
    if (BaseConfig.enhancedShopProtection)

      for (BlockState block : event.getBlocks()) {
        ShopViewer viewer = Shop.getManager().getLoadedShopFrom(block.getLocation());

        if (viewer.isPresent()) {
          event.setCancelled(true);
          return;
        }
      }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onSpongeing(SpongeAbsorbEvent event) {
    if (BaseConfig.enhancedShopProtection)
      for (BlockState block : event.getBlocks())
        Shop.getManager()
        .getLoadedShopFrom(block.getLocation()).ifPresent(() -> event.setCancelled(true));
  }
}
