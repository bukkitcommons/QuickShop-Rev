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
      if (this.connection != null && !this.connection.isClosed()) {
        return this.connection;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    if (this.dbFile.exists()) {
      // So we need a new connection
      try {
        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.dbFile);
        return this.connection;
      } catch (ClassNotFoundException | SQLException e) {
        e.printStackTrace();
        return null;
      }
    } else {
      // So we need a new file too.
      try {
        // Create the file
        this.dbFile.createNewFile();
        // Now we won't need a new file, just a connection.
        // This will return that new connection.
        return this.getConnection();
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }
  }
}
