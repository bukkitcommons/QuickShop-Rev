package org.maxgamer.quickshop.database.connector;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SQLiteConnector implements DatabaseConnector {
  private final File dbFile;
  private Connection connection;

  public SQLiteConnector(@NotNull File dbFile) {
    this.dbFile = dbFile;
  }
  
  /**
   * Gets the database connection for executing queries on.
   *
   * @return The database connection
   */
  @Nullable
  @Override
  public Connection getConnection() {
    try {
      // If we have a current connection, fetch it
      if (connection != null && !connection.isClosed())
        return this.connection;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    
    if (!this.dbFile.exists()) {
      try {
        // Create the file
        this.dbFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
        return getConnection();
      }
    }

    try {
      Class.forName("org.sqlite.JDBC");
      return (connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile));
    } catch (ClassNotFoundException | SQLException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void close() throws SQLException {
    if (connection != null)
      connection.close();
  }
}
