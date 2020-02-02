package org.maxgamer.quickshop.database.connector;

import java.sql.Connection;

public interface DatabaseConnector {
  Connection getConnection();
}
