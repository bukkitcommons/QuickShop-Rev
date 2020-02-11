package cc.bukkit.shop;

public abstract class Shop {
  private static ShopPlugin plugin;
  
  public static ShopPlugin instance() {
    return plugin;
  }
  
  /**
   * Set the plugin instance as the global accessor.
   * @param plugin the shop plugin.
   * @return success if it haven't been set.
   */
  public synchronized static boolean setPlugin(ShopPlugin plugin) {
    if (plugin == null) {
      Shop.plugin = plugin;
      return true;
    } else {
      return false;
    }
  }
  
  public static ShopManager getManager() {
    return plugin.getManager();
  }
  
  public static ShopLoader getLoader() {
    return plugin.getLoader();
  }
}
