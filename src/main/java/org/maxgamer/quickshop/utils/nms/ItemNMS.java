package org.maxgamer.quickshop.utils.nms;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.utils.messages.ShopLogger;

public abstract class ItemNMS {
  private static Method craftItemStack_asNMSCopyMethod;
  private static Method itemStack_saveMethod;
  private static Class<?> nbtTagCompoundClass;
  private static boolean disabled;

  static {
    String name = Bukkit.getServer().getClass().getPackage().getName();
    String nmsVersion = name.substring(name.lastIndexOf('.') + 1);

    try {
      craftItemStack_asNMSCopyMethod =
          Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".inventory.CraftItemStack")
              .getDeclaredMethod("asNMSCopy", ItemStack.class);

      nbtTagCompoundClass = Class.forName("net.minecraft.server." + nmsVersion + ".NBTTagCompound");

      itemStack_saveMethod = Class.forName("net.minecraft.server." + nmsVersion + ".ItemStack")
          .getDeclaredMethod("save", nbtTagCompoundClass);

    } catch (Throwable t) {
      disabled = true;
      ShopLogger.instance().info("Failed to hook NMS, item hologram will be disabled.");
      t.printStackTrace();
    }
  }

  /**
   * Save ItemStack to Json passthrough the NMS.
   *
   * @param bStack ItemStack
   * @return The json for ItemStack.
   * @throws Throwable throws
   */
  @Nullable
  public static String toJson(@NotNull ItemStack bStack) throws Throwable {
    if (disabled || bStack.getType() == Material.AIR)
      return null;
    
    Object mcStack = craftItemStack_asNMSCopyMethod.invoke(null, bStack);
    Object nbtTagCompound = nbtTagCompoundClass.newInstance();
    
    itemStack_saveMethod.invoke(mcStack, nbtTagCompound);
    return nbtTagCompound.toString();
  }
}
