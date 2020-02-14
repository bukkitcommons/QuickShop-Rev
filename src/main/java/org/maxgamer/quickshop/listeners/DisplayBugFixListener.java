package org.maxgamer.quickshop.listeners;

import java.util.Collection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.maxgamer.quickshop.configuration.DisplayConfig;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.Util;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DisplayBugFixListener implements Listener {
  @EventHandler(ignoreCancelled = true)
  public void canBuild(BlockCanBuildEvent e) {
    if (!DisplayConfig.displayItems
        // || DisplayItem.getNowUsing(null) != DisplayType.ARMORSTAND
        || e.isBuildable()) {
      return;
    }

    final Collection<Entity> entities =
        e.getBlock().getWorld().getNearbyEntities(e.getBlock().getLocation(), 1.0, 1, 1.0);

    for (Entity entity : entities) {
      if (!(entity instanceof ArmorStand)
          || !ItemUtils.isDisplayItem(((ArmorStand) entity).getItemInHand(), null)) {
        continue;
      }

      e.setBuildable(true);
      Util.debug(
          "Re-set the allowed build flag here because it found the cause of the display-item blocking it before.");
      return;
    }
  }
}
