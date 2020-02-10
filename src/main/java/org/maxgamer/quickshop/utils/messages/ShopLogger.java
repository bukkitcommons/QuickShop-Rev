package org.maxgamer.quickshop.utils.messages;

import org.bukkit.plugin.Plugin;

public class ShopLogger extends ShopPluginLogger {
  public static void initalize(Plugin plugin) {
    new Singleton(plugin);
  }
  
  /**
   * Gets the instance of this logger, need initalizing.
   * @see ShopLogger#initalize(Plugin)
   * @return the shop logger.
   */
  public static ShopLogger instance() {
    return Singleton.INSTANCE;
  }
  
  private static class Singleton {
    private static ShopLogger INSTANCE;
    
    private Singleton(Plugin plugin) {
      INSTANCE = new ShopLogger(plugin);
    }
  }
  
  private ShopLogger(Plugin plugin) {
    super(plugin);
  }
}
