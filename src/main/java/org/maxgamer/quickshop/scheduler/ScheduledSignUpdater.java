package org.maxgamer.quickshop.scheduler;

import java.util.Set;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.Sets;
import cc.bukkit.shop.ContainerShop;

public class ScheduledSignUpdater implements Runnable {
  private static final Set<ContainerShop> signs = Sets.newHashSet();

  public static void schedule(@NotNull ContainerShop shop) {
    signs.add(shop);
  }

  @Override
  public void run() {
    signs.forEach(ContainerShop::setSignText);
  }
}
