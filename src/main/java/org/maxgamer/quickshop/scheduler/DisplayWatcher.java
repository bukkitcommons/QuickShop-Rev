package org.maxgamer.quickshop.scheduler;

import lombok.Data;
import org.bukkit.scheduler.BukkitRunnable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.shop.Shop;

@Data
public class DisplayWatcher {
  // private ArrayList<Shop> pendingCheckDisplay = new ArrayList<>();
  private QuickShop plugin;

  public DisplayWatcher(QuickShop plugin) {
    this.plugin = plugin;
    registerTask();
  }

  private void registerTask() {
    plugin.getLogger().info("Registering DisplayCheck task....");
    if (BaseConfig.displayItems && plugin.getDisplayItemCheckTicks() > 0) {
      new BukkitRunnable() {
        @Override
        public void run() {
          plugin.getShopManager().getLoadedShops().forEach(Shop::checkDisplay);
        }
      }.runTaskTimer(plugin, 1L, plugin.getDisplayItemCheckTicks());
    }
  }
}
