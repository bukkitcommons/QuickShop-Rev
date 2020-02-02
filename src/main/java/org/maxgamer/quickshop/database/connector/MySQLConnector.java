package org.maxgamer.quickshop.database.connector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;

public class MySQLConnector implements DatabaseConnector {

  private static Connection[] POOL;

  private static final int MAX_CONNECTIONS = 8;

  /** The connection properties... user, pass, autoReconnect.. */
  @NotNull
  private final Properties info;

  @NotNull
  private final String url;

  public MySQLConnector(
      @NotNull String host,
      @NotNull String user,
      @NotNull String pass,
      @NotNull String database,
      @NotNull String port,
      boolean useSSL) {
    
    info = new Properties();
    info.setProperty("autoReconnect", "true");
    info.setProperty("user", user);
    info.setProperty("password", pass);
    info.setProperty("useUnicode", "true");
    info.setProperty("characterEncoding", "utf8");
    info.setProperty("useSSL", String.valueOf(useSSL));
    
    this.url = "jdbc:mysql://" + host + ":" + port + "/" + database;
    POOL = new Connection[MAX_CONNECTIONS];
  }

  /**
   * Gets the database connection for executing queries on.
   *
   * @return The database connection
   */
  @NotNull
  @Override
  public Connection getConnection() {
    for (int i = 0; i < MAX_CONNECTIONS; i++) {
      Connection connection = POOL[i];
      
      try {
        if (connection == null || connection.isClosed())
          return POOL[i] = DriverManager.getConnection(url, info);
        if (connection.isValid(0))
          return connection;
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    
    return getConnection();
  }
}
