package org.maxgamer.quickshop.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.DatabaseConfig;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.database.Database;
import cc.bukkit.shop.database.Dispatcher;
import cc.bukkit.shop.database.connector.MySQLConnector;

/**
 * A Util to execute all SQLs.
 */
public class DatabaseHelper {

  @NotNull
  private final Database db;

  @NotNull
  private final Dispatcher dispatcher;

  public DatabaseHelper(@NotNull Dispatcher dispatcher, @NotNull Database db) throws SQLException {
    this.db = db;
    this.dispatcher = dispatcher;
    
    if (!db.hasTable(DatabaseConfig.databasePrefix + "shops")) {
      createShopsTable();
    }
    if (!db.hasTable(DatabaseConfig.databasePrefix + "messages")) {
      createMessagesTable();
    }
    
    checkColumns();
  }

  /**
   * Verifies that all required columns exist.
   */
  private void checkColumns() {
    PreparedStatement ps;
    try {
      // V3.4.2
      ps = db.getConnection().prepareStatement("ALTER TABLE " + DatabaseConfig.databasePrefix
          + "shops MODIFY COLUMN price double(32,2) NOT NULL AFTER owner");
      ps.execute();
      ps.close();
    } catch (SQLException e) {
      // ignore
    }
    try {
      // V3.4.3
      ps = db.getConnection().prepareStatement("ALTER TABLE " + DatabaseConfig.databasePrefix
          + "messages MODIFY COLUMN time BIGINT(32) NOT NULL AFTER message");
      ps.execute();
      ps.close();
    } catch (SQLException e) {
      // ignore
    }
    if (QuickShop.instance().getDatabase().getConnector() instanceof MySQLConnector) {
      try {
        ps = db.getConnection().prepareStatement("ALTER TABLE " + DatabaseConfig.databasePrefix
            + "messages MODIFY COLUMN message text CHARACTER SET utf8mb4 NOT NULL AFTER owner");
        ps.execute();
        ps.close();
      } catch (SQLException e) {
        // ignore
      }
    }

  }

  public void cleanMessage(long weekAgo) {
    String sqlString = "DELETE FROM " + DatabaseConfig.databasePrefix + "messages WHERE time < ?";
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(weekAgo));
    dispatcher.add(sqlString);
  }
  
  private static String proc(String string) {
    return "'".concat(string).concat("'");
  }

  public void cleanMessageForPlayer(@NotNull UUID player) {
    String sqlString = "DELETE FROM " + DatabaseConfig.databasePrefix + "messages WHERE owner = ?";
    sqlString = StringUtils.replaceOnce(sqlString, "?", proc(player.toString()));
    dispatcher.add(sqlString);
  }

  /**
   * Creates the database table 'messages'
   *
   * @return Create failed or successed.
   * @throws SQLException If the connection is invalid
   */
  private boolean createMessagesTable() throws SQLException {
    Statement st = db.getConnection().createStatement();
    String createTable = "CREATE TABLE " + DatabaseConfig.databasePrefix
        + "messages (owner  VARCHAR(255) NOT NULL, message  TEXT(25) NOT NULL, time  BIGINT(32) NOT NULL );";
    if (db.getConnector() instanceof MySQLConnector) {
      createTable = "CREATE TABLE " + DatabaseConfig.databasePrefix
          + "messages (owner  VARCHAR(255) NOT NULL, message  TEXT(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL , time  BIGINT(32) NOT NULL );";
    }
    return st.execute(createTable);
  }

  public void createShop(@NotNull String owner, double price, @NotNull ItemStack item,
      int unlimited, int shopType, @NotNull String world, int x, int y, int z) throws SQLException {
    deleteShop(x, y, z, world); // First purge old exist shop before create new shop.
    
    Util.debug("Creates shop");
    String sqlString = "INSERT INTO " + DatabaseConfig.databasePrefix
        + "shops (owner, price, itemConfig, x, y, z, world, unlimited, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    sqlString = StringUtils.replaceOnce(sqlString, "?", proc(owner));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(price));
    sqlString = StringUtils.replaceOnce(sqlString, "?", proc(Util.serialize(item)));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(x));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(y));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(z));
    sqlString = StringUtils.replaceOnce(sqlString, "?", proc(world));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(unlimited));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(shopType));
    
    dispatcher.add(sqlString);
  }

  /**
   * Creates the database table 'shops'.
   *
   * @throws SQLException If the connection is invalid.
   */
  private void createShopsTable() throws SQLException {
    Statement st = db.getConnection().createStatement();
    String createTable = "CREATE TABLE " + DatabaseConfig.databasePrefix
        + "shops (owner  VARCHAR(255) NOT NULL, price  double(32, 2) NOT NULL, itemConfig TEXT CHARSET utf8 NOT NULL, x  INTEGER(32) NOT NULL, y  INTEGER(32) NOT NULL, z  INTEGER(32) NOT NULL, world VARCHAR(32) NOT NULL, unlimited  boolean, type  boolean, PRIMARY KEY (x, y, z, world) );";
    st.execute(createTable);
  }

  public void deleteShop(int x, int y, int z, @NotNull String worldName) throws SQLException {
    String sqlString = "DELETE FROM " + DatabaseConfig.databasePrefix
        + "shops WHERE x = ? AND y = ? AND z = ? AND world = ?"
        + (db.getConnector() instanceof MySQLConnector ? " LIMIT 1" : "");
    
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(x));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(y));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(z));
    sqlString = StringUtils.replaceOnce(sqlString, "?", proc(worldName));

    dispatcher.add(sqlString);
  }

  public ResultSet selectAllMessages() throws SQLException {
    Statement st = db.getConnection().createStatement();
    String selectAllShops = "SELECT * FROM " + DatabaseConfig.databasePrefix + "messages";
    return st.executeQuery(selectAllShops);
  }

  public ResultSet selectAllShops() throws SQLException {
    Statement st = db.getConnection().createStatement();
    String selectAllShops = "SELECT * FROM " + DatabaseConfig.databasePrefix + "shops";
    return st.executeQuery(selectAllShops);
  }

  public void sendMessage(@NotNull UUID player, @NotNull String message, long time) {
    String sqlString = "INSERT INTO " + DatabaseConfig.databasePrefix
        + "messages (owner, message, time) VALUES (?, ?, ?)";
    
    sqlString = StringUtils.replaceOnce(sqlString, "?", proc(player.toString()));
    sqlString = StringUtils.replaceOnce(sqlString, "?", proc(message));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(time));
    
    dispatcher.add(sqlString);
  }

  public void updateOwner2UUID(@NotNull String ownerUUID, int x, int y, int z,
      @NotNull String worldName) throws SQLException {
    // QuickShop.instance().getDB().getConnection().createStatement()
    // .executeUpdate("UPDATE " + QuickShop.instance().getDbPrefix() + "shops SET owner = \"" +
    // ownerUUID.toString()
    // + "\" WHERE x = " + x + " AND y = " + y + " AND z = " + z
    // + " AND world = \"" + worldName + "\" LIMIT 1");
    String sqlString = "UPDATE " + DatabaseConfig.databasePrefix
        + "shops SET owner = ? WHERE x = ? AND y = ? AND z = ? AND world = ?"
        + (db.getConnector() instanceof MySQLConnector ? " LIMIT 1" : "");
    
    sqlString = StringUtils.replaceOnce(sqlString, "?", proc(ownerUUID));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(x));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(y));
    sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(z));
    sqlString = StringUtils.replaceOnce(sqlString, "?", proc(worldName));
    
    dispatcher.add(sqlString);
  }

  public void updateShop(@NotNull String owner, @NotNull ItemStack item, int unlimited,
      int shopType, double price, int x, int y, int z, String world) throws SQLException {
    
      String sqlString = "UPDATE " + DatabaseConfig.databasePrefix
          + "shops SET owner = ?, itemConfig = ?, unlimited = ?, type = ?, price = ? WHERE x = ? AND y = ? and z = ? and world = ?";
      
      sqlString = StringUtils.replaceOnce(sqlString, "?", proc(owner));
      sqlString = StringUtils.replaceOnce(sqlString, "?", proc(Util.serialize(item)));
      sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(unlimited));
      sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(shopType));
      sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(price));
      sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(x));
      sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(y));
      sqlString = StringUtils.replaceOnce(sqlString, "?", String.valueOf(z));
      sqlString = StringUtils.replaceOnce(sqlString, "?", proc(world));
      
      dispatcher.add(sqlString);
  }
}
