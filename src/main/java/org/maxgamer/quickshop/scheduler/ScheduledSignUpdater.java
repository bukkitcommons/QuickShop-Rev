package org.maxgamer.quickshop.scheduler;

import java.util.Set;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.Sets;
import cc.bukkit.shop.ChestShop;

public class ScheduledSignUpdater implements Runnable {
    private static final Set<ChestShop> signs = Sets.newHashSet();
    
    public static void schedule(@NotNull ChestShop shop) {
        signs.add(shop);
    }
    
    @Override
    public void run() {
        signs.forEach(ChestShop::setSignText);
    }
}
