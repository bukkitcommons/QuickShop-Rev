package org.maxgamer.quickshop.configuration;

import cc.bukkit.shop.configuration.annotation.Configuration;
import cc.bukkit.shop.configuration.annotation.Node;

@Configuration("configs/display.yml")
public class DisplayConfig {
  @Node("settings.enable")
  public static boolean displayItems = true;
  
  @Node(value = "settings.type")
  public static String displayType = "DROPPED_ITEM";
}
