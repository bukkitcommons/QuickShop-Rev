package org.maxgamer.quickshop.configuration.impl;

import org.maxgamer.quickshop.configuration.Configuration;
import org.maxgamer.quickshop.configuration.Node;

@Configuration("configs/display.yml")
public class DisplayConfig {
  @Node("settings.enable")
  public static boolean displayItems = true;
  
  @Node(value = "settings.type")
  public static String displayType = "DROPPED_ITEM";
}
