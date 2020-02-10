package org.maxgamer.quickshop.scheduler;

import java.util.Set;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.Sets;
import cc.bukkit.shop.Shop;

public class ScheduledSignUpdater implements Runnable {
  private Set<Shop> signs = Sets.newHashSet();

  public void schedule(@NotNull Shop shop) {
    signs.add(shop);
  }

  @Override
  public void run() {
    signs.forEach(Shop::setSignText);
  }
}
