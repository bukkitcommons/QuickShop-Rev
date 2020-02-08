package org.maxgamer.quickshop.shop.hologram.impl;

import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.event.ShopDisplayItemDespawnEvent;
import org.maxgamer.quickshop.event.ShopDisplayItemSpawnEvent;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.shop.api.ShopProtectionFlag;
import org.maxgamer.quickshop.shop.hologram.DisplayAttribute;
import org.maxgamer.quickshop.shop.hologram.DisplayData;
import org.maxgamer.quickshop.shop.hologram.DisplayItem;
import org.maxgamer.quickshop.shop.hologram.DisplayType;
import org.maxgamer.quickshop.utils.Util;

@ToString
public class ArmorStandDisplayItem extends EntityDisplayItem implements DisplayItem {
  public ArmorStandDisplayItem(@NotNull Shop shop, @NotNull DisplayData data) {
    super(shop, data);
  }

  @Override
  public boolean isDisplayItem(@NotNull Entity entity) {
    if (!(entity instanceof ArmorStand))
      return false;
    
    return DisplayItem.isDisplayItem(
        ((ArmorStand) entity).getItem(data.get(DisplayAttribute.SLOT, EquipmentSlot.HEAD)), null);
  }

  @Override
  public void spawn() {
    if (shop.getLocation().getWorld() == null) {
      Util.debug("Canceled the displayItem spawning because the location in the world is null.");
      return;
    }

    if (originalItemStack == null) {
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
        new ShopDisplayItemSpawnEvent(shop, originalItemStack, data);
    Bukkit.getPluginManager().callEvent(shopDisplayItemSpawnEvent);
    if (shopDisplayItemSpawnEvent.isCancelled()) {
      Util.debug(
          "Canceled the displayItem from spawning because a plugin setCancelled the spawning event, usually it is a QuickShop Add on");
      return;
    }

    Location location = getDisplayLocation();
    this.entity = (ArmorStand) this.shop.getLocation().getWorld().spawn(location,
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
    // Set safeGuard
    Util.debug("Spawned armor stand @ " + this.entity.getLocation() + " with UUID "
        + this.entity.getUniqueId());
    safeGuard(this.entity); // Helmet must be set after spawning
  }

  @Override
  public void safeGuard(@NotNull Entity entity) {
    if (!(entity instanceof ArmorStand)) {
      Util.debug(
          "Failed to safeGuard " + entity.getLocation() + ", cause target not a ArmorStand");
      return;
    }
    ArmorStand armorStand = (ArmorStand) entity;
    // Set item protect in the armorstand's hand
    this.guardedIstack = DisplayItem.createGuardItemStack(this.originalItemStack, this.shop);
    armorStand.setItem(data.get(DisplayAttribute.SLOT, EquipmentSlot.HEAD), guardedIstack);
  }

  @Override
  public void remove() {
    super.remove();
    ShopDisplayItemDespawnEvent shopDisplayItemDespawnEvent =
        new ShopDisplayItemDespawnEvent(this.shop, this.originalItemStack, DisplayType.ARMORSTAND);
    Bukkit.getPluginManager().callEvent(shopDisplayItemDespawnEvent);
  }

  @Override
  public Location getDisplayLocation() {
    BlockFace containerBlockFace = BlockFace.NORTH; // Set default vaule
    if (this.shop.getLocation().getBlock().getBlockData() instanceof Directional) {
      containerBlockFace =
          ((Directional) this.shop.getLocation().getBlock().getBlockData()).getFacing(); // Replace
                                                                                         // by
                                                                                         // container
                                                                                         // face.
    }

    // Fix specific block facing
    Material type = this.shop.getLocation().getBlock().getType();
    if (type.name().contains("ANVIL") || type.name().contains("FENCE")
        || type.name().contains("WALL")) {
      switch (containerBlockFace) {
        case SOUTH:
          containerBlockFace = BlockFace.WEST;
          break;
        case NORTH:
          containerBlockFace = BlockFace.EAST;
        case EAST:
          containerBlockFace = BlockFace.NORTH;
        case WEST:
          containerBlockFace = BlockFace.SOUTH;
        default:
          break;
      }
    }

    Location asloc = Util.getCenter(this.shop.getLocation());
    Util.debug("containerBlockFace " + containerBlockFace);

    if (this.originalItemStack.getType().isBlock()) {
      asloc.add(0, 0.5, 0);
    }

    switch (containerBlockFace) {
      case SOUTH:
        asloc.add(0, -0.5, 0);
        asloc.setYaw(0);
        Util.debug("Block face as SOUTH");
        break;
      case WEST:
        asloc.add(0, -0.5, 0);
        asloc.setYaw(90);
        Util.debug("Block face as WEST");
        break;
      case EAST:
        asloc.add(0, -0.5, 0);
        asloc.setYaw(-90);
        Util.debug("Block face as EAST");
        break;
      case NORTH:
        asloc.add(0, -0.5, 0);
        asloc.setYaw(180);
        Util.debug("Block face as NORTH");
        break;
      default:
        break;
    }

    asloc.setYaw(asloc.getYaw() + data.get(DisplayAttribute.OFFSET_YAW, 0f));
    asloc.setPitch(asloc.getYaw() + data.get(DisplayAttribute.OFFSET_PITCH, 0f));
    asloc.add(data.get(DisplayAttribute.OFFSET_X, 0d),
        data.get(DisplayAttribute.OFFSET_Y, 0d),
        data.get(DisplayAttribute.OFFSET_Z, 0d));

    return asloc;
  }
}
