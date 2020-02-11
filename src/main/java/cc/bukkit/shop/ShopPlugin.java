package cc.bukkit.shop;

import org.bukkit.plugin.Plugin;

public interface ShopPlugin extends Plugin {
  ShopManager getManager();
  
  ShopLoader getLoader();
}
