/*
 * This file is a part of project QuickShop, the name is DatabaseHelper.java Copyright (C) Ghost_chu
 * <https://github.com/Ghost-chu> Copyright (C) Bukkit Commons Studio and contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.maxgamer.quickshop.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.configuration.impl.DatabaseConfig;
import org.maxgamer.quickshop.database.connector.MySQLConnector;
import org.maxgamer.quickshop.utils.Util;

/**
 * A Util to execute all SQLs.
 */
public class DatabaseHelper {

  @NotNull
  private final Database db;

  @NotNull
  private final QuickShop plugin;

  public DatabaseHelper(@NotNull QuickShop plugin, @NotNull Database db) throws SQLException {
    this.db = db;
    this.plugin = plugin;
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
    try {
      // QuickShop.instance().getDB().execute("DELETE FROM " + QuickShop.instance
      // .getDbPrefix() + "messages WHERE time < ?", weekAgo);
      String sqlString = "DELETE FROM " + DatabaseConfig.databasePrefix + "messages WHERE time < ?";
      PreparedStatement ps = db.getConnection().prepareStatement(sqlString);
      ps.setLong(1, weekAgo);
      plugin.getDatabaseManager().add(ps);
    } catch (SQLException sqle) {
      sqle.printStackTrace();
    }
  }

  public void cleanMessageForPlayer(@NotNull UUID player) {
    try {
      String sqlString = "DELETE FROM " + DatabaseConfig.databasePrefix + "messages WHERE owner = ?";
      PreparedStatement ps = db.getConnection().prepareStatement(sqlString);
      ps.setString(1, player.toString());
      plugin.getDatabaseManager().add(ps);
    } catch (SQLException sqle) {
      sqle.printStackTrace();
    }
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
    if (plugin.getDatabase().getConnector() instanceof MySQLConnector) {
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
    // QuickShop.instance().getDB().execute(q, owner, price, Util.serialize(item), x, y, z, world,
    // unlimited, shopType);
    PreparedStatement ps = db.getConnection().prepareStatement(sqlString);
    ps.setString(1, owner);
    ps.setDouble(2, price);
    ps.setString(3, Util.serialize(item));
    ps.setInt(4, x);
    ps.setInt(5, y);
    ps.setInt(6, z);
    ps.setString(7, world);
    ps.setInt(8, unlimited);
    ps.setInt(9, shopType);
    plugin.getDatabaseManager().add(ps);
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

  public boolean deleteShop(int x, int y, int z, @NotNull String worldName) throws SQLException {
    Util.debug("Deletes shop");
    
    String sqlString = "DELETE FROM " + DatabaseConfig.databasePrefix
        + "shops WHERE x = ? AND y = ? AND z = ? AND world = ?"
        + (db.getConnector() instanceof MySQLConnector ? " LIMIT 1" : "");

    PreparedStatement ps = db.getConnection().prepareStatement(sqlString);
    ps.setInt(1, x);
    ps.setInt(2, y);
    ps.setInt(3, z);
    ps.setString(4, worldName);
    return ps.execute();
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
    try {
      String sqlString = "INSERT INTO " + DatabaseConfig.databasePrefix
          + "messages (owner, message, time) VALUES (?, ?, ?)";
      // QuickShop.instance().getDB().execute(q, player.toString(), message,
      // System.currentTimeMillis());
      PreparedStatement ps = db.getConnection().prepareStatement(sqlString);
      ps.setString(1, player.toString());
      ps.setString(2, message);
      ps.setLong(3, time);
      plugin.getDatabaseManager().add(ps);
    } catch (SQLException sqle) {
      sqle.printStackTrace();
    }
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
    PreparedStatement ps = db.getConnection().prepareStatement(sqlString);
    ps.setString(1, ownerUUID);
    ps.setInt(2, x);
    ps.setInt(3, y);
    ps.setInt(4, z);
    ps.setString(5, worldName);
    plugin.getDatabaseManager().add(ps);
  }

  public void updateShop(@NotNull String owner, @NotNull ItemStack item, int unlimited,
      int shopType, double price, int x, int y, int z, String world) throws SQLException {
    
      String sqlString = "UPDATE " + DatabaseConfig.databasePrefix
          + "shops SET owner = ?, itemConfig = ?, unlimited = ?, type = ?, price = ? WHERE x = ? AND y = ? and z = ? and world = ?";
      PreparedStatement ps = db.getConnection().prepareStatement(sqlString);
      ps.setString(1, owner);
      ps.setString(2, Util.serialize(item));
      ps.setInt(3, unlimited);
      ps.setInt(4, shopType);
      ps.setDouble(5, price);
      ps.setInt(6, x);
      ps.setInt(7, y);
      ps.setInt(8, z);
      ps.setString(9, world);
      plugin.getDatabaseManager().add(ps);
      // db.execute(q, owner, Util.serialize(item), unlimited, shopType, price, x, y, z, world);
  }
}
