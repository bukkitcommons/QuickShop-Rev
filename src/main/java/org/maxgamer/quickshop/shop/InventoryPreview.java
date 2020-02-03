package org.maxgamer.quickshop.shop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.event.ShopInventoryPreviewEvent;
import org.maxgamer.quickshop.utils.MsgUtil;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Lists;

/** A class to create a GUI item preview quickly */
@EqualsAndHashCode
@ToString
public class InventoryPreview implements Listener {

  @Nullable
  private Inventory inventory;
  private ItemStack itemStack;
  private Player player;
  
  private boolean noShow; // FIXME use nbt tag

  /**
   * Create a preview item GUI for a player.
   *
   * @param itemStack The item you want create.
   * @param player Target player.
   */
  public InventoryPreview(@NotNull ItemStack itemStack, @NotNull Player player) {
    this.itemStack = new ItemStack(itemStack);
    this.player = player;
    
    if (itemStack.hasItemMeta()) {
      ItemMeta meta = itemStack.getItemMeta();
      
      if (meta.hasLore()) {
        List<String> lore = meta.getLore();
        lore.add("QuickShop GUI preview item");
        meta.setLore(lore);
      } else {
        meta.setLore(Collections.singletonList("QuickShop GUI preview item"));
      }
      
      this.itemStack.setItemMeta(meta);
    } else {
      noShow = true;
    }
  }

  public static boolean isPreviewItem(@Nullable ItemStack stack) {
    if (stack == null) {
      return false;
    }
    if (!stack.hasItemMeta() || !stack.getItemMeta().hasLore()) {
      return false;
    }
    List<String> lores = stack.getItemMeta().getLore();
    for (String string : lores) {
      if ("QuickShop GUI preview item".equals(string)) {
        return true;
      }
    }
    return false;
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
    if (noShow)
      return;
    if (inventory != null) // Not inited
    {
      close();
    }
    if (itemStack == null) // Null pointer exception
    {
      return;
    }
    if (player == null) // Null pointer exception
    {
      return;
    }
    if (player.isSleeping()) // Bed bug
    {
      return;
    }
    ShopInventoryPreviewEvent shopInventoryPreview =
        new ShopInventoryPreviewEvent(player, itemStack);
    Bukkit.getPluginManager().callEvent(shopInventoryPreview);
    if (shopInventoryPreview.isCancelled()) {
      Util.debugLog("Inventory preview was canceled by a plugin.");
      return;
    }
    final int size = 9;
    inventory = Bukkit.createInventory(null, size, MsgUtil.getMessage("menu.preview", player));
    for (int i = 0; i < size; i++) {
      inventory.setItem(i, itemStack);
    }
    player.openInventory(inventory);
  }
}
