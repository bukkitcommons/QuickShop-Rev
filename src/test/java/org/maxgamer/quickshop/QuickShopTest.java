package org.maxgamer.quickshop;

import java.io.File;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

public final class QuickShopTest extends JavaPlugin {
    
    public QuickShopTest() {}
    
    public QuickShopTest(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, new File("build"), file);
    }
    
    @Override
    public void onLoad() {
        
    }
    
    @Override
    public void onEnable() {
        
    }
    
    @Override
    public void onDisable() {
        
    }
}
