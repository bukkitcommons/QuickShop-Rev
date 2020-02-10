package cc.bukkit.shop.database.connector;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnector {
  Connection getConnection();
  
  void close() throws SQLException;
}
