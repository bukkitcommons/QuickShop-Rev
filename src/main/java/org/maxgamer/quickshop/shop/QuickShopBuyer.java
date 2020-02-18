package org.maxgamer.quickshop.shop;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.utils.ItemUtils;
import cc.bukkit.shop.ShopType;
import cc.bukkit.shop.buyer.ChestBuyer;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.misc.ShopLocation;
import cc.bukkit.shop.moderator.ShopModerator;
import cc.bukkit.shop.stack.ItemStacked;
import cc.bukkit.shop.stack.Stack;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuickShopBuyer extends ContainerQuickShop implements ChestBuyer {
  
  public QuickShopBuyer(@NotNull ShopLocation shopLocation, Stack price,
      @NotNull ItemStacked item, @NotNull ShopModerator moderator, boolean unlimited,
      @NotNull ShopType type) {
    super(shopLocation, price, item, moderator, unlimited, type);
  }

  /**
   * Buys amount of item from Player p. Does NOT check our inventory, or balances
   *
   * @param p The player to buy from
   * @param stackAmount The amount to buy
   */
  @Override
  public void buy(@NotNull Player p, int stackAmount) {
    if (stackAmount <= 0)
      return;
    
    int totalAmount = stackAmount * stack.stack().getAmount();
    
    Inventory playerInv = p.getInventory();
    ItemStack[] contents = playerInv.getStorageContents();
    
    for (int i = 0; totalAmount > 0 && i < contents.length; i++) {
      ItemStack playerItem = contents[i];
      
      if (playerItem == null || !isStack(playerItem)) {
        continue;
      }
      
      int buyAmount = Math.min(totalAmount, playerItem.getAmount());
      playerItem.setAmount(playerItem.getAmount() - buyAmount);
      totalAmount -= buyAmount;
    }
    
    playerInv.setStorageContents(contents);
    
    if (totalAmount > 0) {
      ShopLogger.instance().severe("Could not take all items from a players inventory on purchase! " + p.getName()
              + ", missing: " + stackAmount + ", item: " + ItemUtils.getItemStackName(stack.stack())
              + "!");
    } else if (!unlimited) {
      ItemStack offer = new ItemStack(stack.stack());
      offer.setAmount(stackAmount * stack.stack().getAmount());
      getInventory().addItem(offer);
      
      setSignText();
    }
  }

  @Override
  public @NotNull ShopType type() {
    return ShopType.BUYING;
  }
}
