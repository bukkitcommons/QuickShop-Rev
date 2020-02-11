package org.maxgamer.quickshop.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.event.ShopCreateEvent;
import cc.bukkit.shop.event.ShopDeleteEvent;
import cc.bukkit.shop.event.ShopModeratorChangedEvent;
import cc.bukkit.shop.event.ShopPriceChangeEvent;
import cc.bukkit.shop.event.ShopSuccessPurchaseEvent;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InternalListener implements Listener {
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void shopCreate(ShopCreateEvent event) {
    Util.debug("Player " + event.getPlayer().getName() + " created a shop at location "
        + event.getShop().getLocation());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void shopDelete(ShopDeleteEvent event) {
    Util.debug("Shop at " + event.getShop().getLocation() + " was removed.");
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void shopModeratorChanges(ShopModeratorChangedEvent event) {
    Util.debug("Shop at location " + event.getShop().getLocation() + " moderator was changed to "
        + event.getModerator());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void shopPriceChanges(ShopPriceChangeEvent event) {
    Util.debug("Shop at location " + event.getShop().getLocation() + " price was changed from "
        + event.getOldPrice() + " to " + event.getNewPrice());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void shopPurchase(ShopSuccessPurchaseEvent event) {
    if (event.getShop().getShopType() == ShopType.BUYING) {
      Util.debug("Player " + event.getPlayer().getName() + " sold " + event.getShop().ownerName()
          + " shop " + event.getShop() + " for items x" + event.getAmount() + " for "
          + QuickShop.instance().getEconomy().format(event.getBalance()) + " ("
          + QuickShop.instance().getEconomy().format(event.getTax()) + " tax).");
    }
    if (event.getShop().getShopType() == ShopType.SELLING) {
      Util.debug("Player " + event.getPlayer().getName() + " bought " + event.getShop().ownerName()
          + " shop " + event.getShop() + " for items x" + event.getAmount() + " for "
          + QuickShop.instance().getEconomy().format(event.getBalance()) + " ("
          + QuickShop.instance().getEconomy().format(event.getTax()) + " tax).");
    }
  }
}
