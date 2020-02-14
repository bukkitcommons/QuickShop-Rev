package org.maxgamer.quickshop.utils;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EnderChest;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.hologram.EntityDisplay;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import cc.bukkit.shop.ContainerShop;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.hologram.DisplayInfo;
import cc.bukkit.shop.viewer.ShopViewer;
import lombok.SneakyThrows;

public class ShopUtils {
  @NotNull
  private final static DecimalFormat decimalFormat = new DecimalFormat(BaseConfig.decimalFormat);
  
  public static String formatPrice(double d) {
    return decimalFormat.format(d);
  }
  
  /**
   * Gets the shop a sign is attached to
   *
   * @param b The location of the sign
   * @return The shop
   */
  public static ShopViewer getShopBySign(@NotNull Block sign) {
    final Optional<Block> shopBlock = BlockUtils.getSignAttached(sign);
    
    return shopBlock.isPresent() ?
        Shop.getManager()
          .getLoadedShopAt(shopBlock.get()) : ShopViewer.empty();
  }

  public static boolean canBeShop(@NotNull World world, int x, int y, int z) {
    BlockState state = world.getBlockAt(x, y, z).getState();
    return canBeShop(state);
  }
  
  public static boolean canBeShop(@NotNull Block block) {
    // Specificed types by configuration
    if (Util.isBlocklisted(block.getType()) || Util.isBlacklistWorld(block.getWorld())) {
      return false;
    }
    return canBeShopIgnoreBlocklist(block.getState());
  }
  
  public static boolean canBeShop(@NotNull BlockState state) {
    if (Util.isBlocklisted(state.getType()) || Util.isBlacklistWorld(state.getWorld())) {
      return false;
    }
    return canBeShopIgnoreBlocklist(state);
  }

  /**
   * Returns true if the given block could be used to make a shop out of.
   *
   * @param b The block to check, Possibly a chest, dispenser, etc.
   * @return True if it can be made into a shop, otherwise false.
   */
  public static boolean canBeShopIgnoreBlocklist(@NotNull BlockState state) {
    if (state instanceof EnderChest) { // BlockState for Mod supporting
      return QuickShop.instance().getOpenInvPlugin() != null;
    }

    return state instanceof InventoryHolder;
  }

  /**
   * Counts the number of items in the given inventory where Util.matches(inventory item, item) is
   * true.
   *
   * @param inv The inventory to search
   * @param item The ItemStack to search for
   * @return The number of items that match in this inventory.
   */
  public static int countStacks(@Nullable Inventory inv, @NotNull ItemStack item) {
    if (inv == null)
      return 0;
    
    int items = 0;
    for (ItemStack iStack : inv.getStorageContents()) {
      if (iStack == null || iStack.getType() == Material.AIR)
        continue;
      
      if (QuickShop.instance().getItemMatcher().matches(item, iStack, false)) {
        items += iStack.getAmount();
      }
    }
    
    return items / item.getAmount();
  }

  /**
   * Returns the number of items that can be given to the inventory safely.
   *
   * @param inv The inventory to count
   * @param item The item prototype. Material, durabiltiy and enchants must match for 'stackability'
   *        to occur.
   * @return The number of items that can be given to the inventory safely.
   */
  public static int countSpace(@Nullable Inventory inv, @NotNull ItemStack item) {
    if (inv == null)
      return 0;
    
    int space = 0;

    ItemStack[] contents = inv.getStorageContents();
    for (ItemStack iStack : contents) {
      if (iStack == null || iStack.getType() == Material.AIR) {
        space += item.getMaxStackSize();
      } else if (QuickShop.instance().getItemMatcher().matches(item, iStack, false)) {
        space += (item.getMaxStackSize() - iStack.getAmount());
      }
    }
    
    return space;
  }

  /**
   * Checks whether someone else's shop is within reach of a hopper being placed by a player.
   *
   * @param b The block being placed.
   * @param p The player performing the action.
   * @return true if a nearby shop was found, false otherwise.
   */
  public static boolean isOtherShopWithinHopperReach(@NotNull Block b, @NotNull Player p) {
    // Check 5 relative positions that can be affected by a hopper: behind, in front of, to the
    // right,
    // to the left and underneath.
    Block[] blocks = new Block[5];
    blocks[0] = b.getRelative(0, 0, -1);
    blocks[1] = b.getRelative(0, 0, 1);
    blocks[2] = b.getRelative(1, 0, 0);
    blocks[3] = b.getRelative(-1, 0, 0);
    blocks[4] = b.getRelative(0, 1, 0);
    for (Block c : blocks) {
      ShopViewer firstShop = Shop.getManager().getLoadedShopAt(c.getLocation());
      // If firstShop is null but is container, it can be used to drain contents from a shop created
      // on secondHalf.
      Optional<Location> secondHalf = BlockUtils.getSecondHalf(c);
      ShopViewer secondShop =
          secondHalf.isPresent() ?
              Shop.getManager().getLoadedShopAt(secondHalf.get()) : ShopViewer.empty();
      
      if (firstShop.isPresent() && !p.getUniqueId().equals(firstShop.get().getOwner())
          || secondShop.isPresent() && !p.getUniqueId().equals(secondShop.get().getOwner())) {
        return true;
      }
    }
    return false;
  }
}
