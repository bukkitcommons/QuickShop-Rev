package org.maxgamer.quickshop.database.connector;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnector {
  Connection getConnection();
  
  void close() throws SQLException;
}
