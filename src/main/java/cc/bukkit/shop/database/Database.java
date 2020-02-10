package cc.bukkit.shop.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import cc.bukkit.shop.database.connector.DatabaseConnector;

public class Database {
  @NotNull
  private final DatabaseConnector connector;

  /**
   * Creates a new database and validates its connection.
   *
   * <p>
   * If the connection is invalid, this will throw a ConnectionException.
   *
   * @param core The core for the database, either MySQL or SQLite.
   * @throws ConnectionException If the connection was invalid
   */
  public Database(@NotNull DatabaseConnector core) throws ConnectionException {
    this.connector = core;
  }

  /**
   * Returns true if the given table has the given column
   *
   * @param table The table
   * @param column The column
   * @return True if the given table has the given column
   * @throws SQLException If the database isn't connected
   */
  public boolean hasColumn(@NotNull String table, @NotNull String column) throws SQLException {
    Connection conn = connector.getConnection();
    if (!hasTable(conn, table))
      return false;
    
    String query = "SELECT * FROM " + table + " LIMIT 0,1";
    try {
      @Cleanup PreparedStatement ps = conn.prepareStatement(query);
      @Cleanup ResultSet rs = ps.executeQuery();
      while (rs.next())
        if (rs.getString(column) != null)
          return true;
      
      return false;
    } catch (Throwable t) {
      return false;
    }
  }
  
  @SneakyThrows
  public boolean hasTable(@NotNull String table) {
    return hasTable(connector.getConnection(),  table);
  }

  /**
   * Returns true if the table exists
   *
   * @param table The table to check for
   * @return True if the table is found
   * @throws SQLException Throw exception when failed execute somethins on SQL
   */
  private boolean hasTable(@NotNull Connection conn, @NotNull String table) {
    try {
      @Cleanup ResultSet rs = conn.getMetaData().getTables(null, null, "%", null);
      while (rs.next())
        if (table.equalsIgnoreCase(rs.getString("TABLE_NAME")))
          return true;
      
      return false;
    } catch (Throwable t) {
      return false;
    }
  }

  /**
   * Fetches the connection to this database for querying. Try to avoid doing this in the main
   * thread.
   *
   * @return Fetches the connection to this database for querying.
   */
  public Connection getConnection() {
    return connector.getConnection();
  }

  /**
   * Returns the database core object, that this database runs on.
   *
   * @return the database core object, that this database runs on.
   */
  @NotNull
  public DatabaseConnector getConnector() {
    return connector;
  }

  /**
   * Represents a connection error, generally when the server can't connect to MySQL or something.
   */
  public static class ConnectionException extends Exception {
    private static final long serialVersionUID = 8348749992936357317L;

    private ConnectionException(String msg) {
      super(msg);
    }
  }
}
