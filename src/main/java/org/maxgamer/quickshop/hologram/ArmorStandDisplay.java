package org.maxgamer.quickshop.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.BlockUtils;
import org.maxgamer.quickshop.utils.ItemUtils;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ChestShop;
import cc.bukkit.shop.event.ShopDisplayItemSpawnEvent;
import cc.bukkit.shop.hologram.DisplayAttribute;
import cc.bukkit.shop.hologram.DisplayData;
import cc.bukkit.shop.hologram.DisplayScheme;
import cc.bukkit.shop.hologram.Display;
import lombok.ToString;

@ToString
public class ArmorStandDisplay extends EntityDisplay implements Display {
  public ArmorStandDisplay(@NotNull ChestShop shop, @NotNull DisplayData data) {
    super(shop, data, null);
  }

  public void spawn() {
    if (shop.location().world() == null) {
      Util.debug("Canceled the displayItem spawning because the location in the world is null.");
      return;
    }

    if (displayItemStack == null) {
      Util.debug("Canceled the displayItem spawning because the ItemStack is null.");
      return;
    }

    if (entity != null && entity.isValid() && !entity.isDead()) {
      Util.debug(
          "Warning: Spawning the armorStand for DisplayItem when there is already an existing armorStand may cause a duplicated armorStand!");
      StackTraceElement[] traces = Thread.currentThread().getStackTrace();
      for (StackTraceElement trace : traces) {
        Util.debug(
            trace.getClassName() + "#" + trace.getMethodName() + "#" + trace.getLineNumber());
      }
    }

    ShopDisplayItemSpawnEvent shopDisplayItemSpawnEvent =
        new ShopDisplayItemSpawnEvent(shop, displayItemStack, (@NotNull DisplayScheme) data);
    Bukkit.getPluginManager().callEvent(shopDisplayItemSpawnEvent);
    if (shopDisplayItemSpawnEvent.isCancelled()) {
      Util.debug(
          "Cancelled the displayItem from spawning because a plugin setCancelled the spawning event, usually it is a QuickShop Add on");
      return;
    }

    /*
    Location location = getDisplayLocation();
    this.entity = this.shop.getLocation().world().spawn(location,
        ArmorStand.class, armorStand -> {
          // Set basic armorstand datas.
          armorStand.setGravity(false);
          armorStand.setVisible(false);
          armorStand.setMarker(true);
          armorStand.setInvulnerable(true);
          armorStand.setCollidable(false);
          armorStand.setSmall(data.get(DisplayAttribute.SMALL, true));
          armorStand.setArms(false);
          armorStand.setBasePlate(false);
          armorStand.setSilent(true);
          armorStand.setAI(false);
          armorStand.setCanMove(false);
          armorStand.setCanPickupItems(false);
          // Set pose
          data.setPoseForArmorStand(armorStand);
        });
    */
    Util.debug("Spawned armor stand @ " + this.entity.getLocation() + " with UUID "
        + this.entity.getUniqueId());
    // Helmet must be set after spawning
    ArmorStand armorStand = (ArmorStand) entity;
    armorStand.setItem(data.get(DisplayAttribute.SLOT, EquipmentSlot.HEAD), displayItemStack);
  }
}
