package org.maxgamer.quickshop.shop;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.Lists;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.hologram.DisplayScheme;
import cc.bukkit.shop.misc.ShopLocation;
import cc.bukkit.shop.moderator.ShopModerator;
import cc.bukkit.shop.seller.ChestSeller;
import cc.bukkit.shop.stack.ItemStacked;
import cc.bukkit.shop.stack.Stack;

public class QuickShopSeller extends ContainerQuickShop implements ChestSeller {
  public QuickShopSeller(@NotNull ShopLocation shopLocation, Stack price,
      @NotNull ItemStacked item, @NotNull ShopModerator moderator, boolean unlimited,
      @NotNull ShopType type) {
    super(shopLocation, price, item, moderator, unlimited, type);
  }

  /**
   * Sells amount of item to Player p. Does NOT check our inventory, or balances
   *
   * @param p The player to sell to
   * @param stackAmount The amount to sell
   */
  @Override
  public void sell(@NotNull Player p, int stackAmount) {
    int amount = stackAmount;
    if (amount <= 0)
      return;
    
    // Overslot Items to drop on floor
    List<ItemStack> floor = Lists.newArrayList();
    
    Inventory playerInv = p.getInventory();
    ItemStack offer = new ItemStack(stack.stack());
    
    if (unlimited) {
      while (amount --> 0)
        floor.addAll(playerInv.addItem(offer).values());
      
    } else {
      Inventory chestInv = getInventory();
      ItemStack[] contents = chestInv.getContents();
      
      int totalAmount = amount * stack.stack().getAmount();
      // Take items from chest and offer to player's inventory
      for (int i = 0; totalAmount > 0 && i < contents.length; i++) {
        ItemStack chestItem = contents[i];
        if (chestItem == null || !isStack(chestItem))
          continue;
        
        int takeAmount = Math.min(totalAmount, chestItem.getAmount());
        chestItem.setAmount(chestItem.getAmount() - takeAmount);
        
        offer.setAmount(takeAmount);
        floor.addAll(playerInv.addItem(offer).values());
        
        totalAmount -= takeAmount;
      }
      
      chestInv.setContents(contents);
      setSignText();
    }
    
    for (ItemStack stack : floor) {
      p.getWorld().dropItem(p.getLocation(), stack);
    }
  }

  @Override
  public @NotNull ShopType type() {
    return ShopType.SELLING;
  }
}
