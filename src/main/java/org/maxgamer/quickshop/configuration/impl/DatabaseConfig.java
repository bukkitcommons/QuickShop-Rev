package org.maxgamer.quickshop.configuration.impl;

import org.maxgamer.quickshop.configuration.Configuration;
import org.maxgamer.quickshop.configuration.Node;

@Configuration("configs/database.yml")
public class DatabaseConfig {
  @Node("settings.prefix")
  public static String databasePrefix = "";
  
  @Node("settings.mysql.enable")
  public static boolean enableMySQL = false;
  
  @Node("settings.mysql.ssl")
  public static boolean enableSSL = false;
  
  @Node("settings.mysql.host")
  public static String host = "localhost";
  
  @Node("settings.mysql.port")
  public static String port = "3306";
  
  @Node("settings.mysql.name")
  public static String name = "quickshop";
}
