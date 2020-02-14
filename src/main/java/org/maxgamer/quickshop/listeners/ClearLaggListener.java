package org.maxgamer.quickshop.listeners;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.maxgamer.quickshop.utils.ItemUtils;

public class ClearLaggListener implements Listener {
  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
  public void plugin(me.minebuilders.clearlag.events.EntityRemoveEvent clearlaggEvent) {
    final List<Entity> entities = clearlaggEvent.getEntityList();
    final List<Entity> pendingExclude = new ArrayList<>();

    for (Entity entity : entities) {
      if (!(entity instanceof Item)
          || !ItemUtils.isDisplayItem(((Item) entity).getItemStack())) {
        continue;
      }

      pendingExclude.add(entity);
    }

    pendingExclude.forEach(clearlaggEvent::removeEntity);
  }
}
