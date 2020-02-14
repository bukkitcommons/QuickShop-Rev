package org.maxgamer.quickshop.utils;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;

public class Colorizer {

  private static Map<String, String> colorCodes = new HashMap<>();

  static {
    for (ChatColor color : ChatColor.values()) {
      colorCodes.put(color.name().replace("_", ""), String.valueOf(color.getChar()));
      colorCodes.put(color.name(), String.valueOf(color.getChar()));
    }
  }

  public static String setColors(String text) {
    for (String color : colorCodes.keySet()) {
      text = text.replaceAll("(?i)<" + color + ">", ChatColor.COLOR_CHAR + colorCodes.get(color));
    }
    text = text.replaceAll("(?i)<([0-9a-fk-or])>", ChatColor.COLOR_CHAR + "$1");
    text = ChatColor.translateAlternateColorCodes('&', text);
    return text;
  }

  public static String stripColors(String text) {
    for (String color : colorCodes.keySet()) {
      text = text.replaceAll("(?i)<" + color + ">", "");
    }
    text = text.replaceAll("(?i)<[0-9a-fk-or]>", "");
    text = ChatColor.stripColor(text);
    return text;
  }
}
