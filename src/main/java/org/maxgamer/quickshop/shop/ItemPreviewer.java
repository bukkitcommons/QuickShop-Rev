package org.maxgamer.quickshop.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.event.ShopInventoryPreviewEvent;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** A class to create a GUI item preview quickly */
@EqualsAndHashCode
@ToString
public class ItemPreviewer implements Listener {

  @Nullable
  private Inventory inventory;
  @NotNull
  private ItemStack itemStack;
  @NotNull
  private Player player;

  /**
   * Create a preview item GUI for a player.
   *
   * @param itemStack The item you want create.
   * @param player Target player.
   */
  public ItemPreviewer(@NotNull ItemStack itemStack, @NotNull Player player) {
    this.player = player;
    
    NBTItem nbtItem = new NBTItem(itemStack);
    nbtItem.setBoolean("isQuickShopPreview", Boolean.TRUE);
    this.itemStack = nbtItem.getItem();
  }

  public static boolean isPreviewItem(@Nullable ItemStack stack) {
    if (stack == null || stack.getType() == Material.AIR)
      return false;
    
    NBTItem nbtItem = new NBTItem(stack);
    return nbtItem.hasKey("isQuickShopPreview");
  }

  public void close() {
    if (inventory == null) {
      return;
    }
    for (HumanEntity player : inventory.getViewers()) {
      player.closeInventory();
    }
    inventory = null; // Destory
  }

  /** Open the preview GUI for player. */
  public void show() {
    if (inventory != null)
      close();
    if (player.isSleeping())
      return;
    
    ShopInventoryPreviewEvent shopInventoryPreview = new ShopInventoryPreviewEvent(player, itemStack);
    if (Util.fireCancellableEvent(shopInventoryPreview))
      return;
    
    final int size = 9;
    inventory = Bukkit.createInventory(null, 9, Shop.getLocaleManager().get("menu.preview", player));
    for (int i = 0; i < size; i++) {
      inventory.setItem(i, shopInventoryPreview.getItemStack());
    }
    player.openInventory(inventory);
  }
}
