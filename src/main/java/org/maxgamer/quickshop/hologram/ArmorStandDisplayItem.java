package org.maxgamer.quickshop.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.event.ShopDisplayItemSpawnEvent;
import cc.bukkit.shop.hologram.DisplayAttribute;
import cc.bukkit.shop.hologram.DisplayData;
import cc.bukkit.shop.hologram.DisplayItem;
import lombok.ToString;

@ToString
public class ArmorStandDisplayItem extends EntityDisplayItem implements DisplayItem {
  public ArmorStandDisplayItem(@NotNull ContainerShop shop, @NotNull DisplayData data) {
    super(shop, data);
  }
  
  @Override
  @SuppressWarnings("deprecation")
  public Location getDisplayLocation() {
    if (location != null)
      return location;
    
    BlockFace containerBlockFace = BlockFace.NORTH; // Set default vaule
    
    try {
      if (shop.getLocation().block().getBlockData() instanceof Directional)
        containerBlockFace = ((Directional) shop.getLocation().block().getBlockData()).getFacing();
      
    } catch (Throwable t) {
      org.bukkit.material.MaterialData data = shop.getLocation().block().getState().getData();
      if (data instanceof org.bukkit.material.Chest)
        containerBlockFace = ((org.bukkit.material.Chest) data).getFacing();
      else if (data instanceof org.bukkit.material.EnderChest)
        containerBlockFace = ((org.bukkit.material.EnderChest) data).getFacing();
    }

    Location asloc = Util.getCenter(shop.getLocation());
    if (!displayItemStack.getType().isBlock())
      asloc.add(0, -0.5, 0);

    switch (containerBlockFace) {
      case WEST:
        asloc.setYaw(90);
        break;
      case EAST:
        asloc.setYaw(-90);
        break;
      case NORTH:
        asloc.setYaw(180);
        break;
      case SOUTH:
      default:
        break;
    }
    
    asloc.setYaw(asloc.getYaw() + data.get(DisplayAttribute.OFFSET_YAW, 0f));
    asloc.setPitch(asloc.getYaw() + data.get(DisplayAttribute.OFFSET_PITCH, 0f));
    
    asloc.add(
        data.get(DisplayAttribute.OFFSET_X, 0d),
        data.get(DisplayAttribute.OFFSET_Y, 0d),
        data.get(DisplayAttribute.OFFSET_Z, 0d));
    
    return (location = asloc);
  }

  @Override
  public boolean isDisplayItem(@NotNull Entity entity) {
    if (!(entity instanceof ArmorStand))
      return false;
    
    return Util.isDisplayItem(
        ((ArmorStand) entity).getItem(data.get(DisplayAttribute.SLOT, EquipmentSlot.HEAD)), null);
  }

  @Override
  public void spawn() {
    if (shop.getLocation().world() == null) {
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
        new ShopDisplayItemSpawnEvent(shop, displayItemStack, data);
    Bukkit.getPluginManager().callEvent(shopDisplayItemSpawnEvent);
    if (shopDisplayItemSpawnEvent.isCancelled()) {
      Util.debug(
          "Canceled the displayItem from spawning because a plugin setCancelled the spawning event, usually it is a QuickShop Add on");
      return;
    }

    Location location = getDisplayLocation();
    this.entity = (ArmorStand) this.shop.getLocation().world().spawn(location,
        ArmorStand.class, armorStand -> {
          // Set basic armorstand datas.
          armorStand.setGravity(false);
          armorStand.setVisible(false);
          armorStand.setMarker(true);
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
    Util.debug("Spawned armor stand @ " + this.entity.getLocation() + " with UUID "
        + this.entity.getUniqueId());
    // Helmet must be set after spawning
    ArmorStand armorStand = (ArmorStand) entity;
    armorStand.setItem(data.get(DisplayAttribute.SLOT, EquipmentSlot.HEAD), displayItemStack);
  }
}
