package org.maxgamer.quickshop.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.messages.WarningSender;
import com.google.common.collect.Queues;

/** Queued database manager. Use queue to solve run SQL make server lagg issue. */
public class Dispatcher implements Runnable {
  private final BlockingQueue<PreparedStatement> sqlQueue = Queues.newLinkedBlockingQueue();

  @NotNull
  private final Database database;
  @NotNull
  private final WarningSender warningSender;
  @Nullable
  private volatile boolean running = true;
  @NotNull
  private final Thread dispatcherThread;

  /**
   * Queued database manager. Use queue to solve run SQL make server lagg issue.
   *
   * @param plugin plugin main class
   * @param db database
   */
  public Dispatcher(@NotNull Database db) {
    warningSender = new WarningSender(QuickShop.instance(), 600000);
    database = db;
    
    dispatcherThread = new Thread(this);
    dispatcherThread.setName("QuickShop Database Dispatcher");
    dispatcherThread.setPriority(Thread.MIN_PRIORITY);
    dispatcherThread.start();
  }

  /**
   * Add preparedStatement to queue waiting flush to database,
   *
   * @param ps The ps you want add in queue.
   */
  public void add(@NotNull PreparedStatement ps) {
    if (running)
      sqlQueue.offer(ps);
    else
      execute(ps);
  }

  @Override
  public void run() {
    try {
      while (running)
        execute(sqlQueue.take());
    } catch (InterruptedException interrupted) {
      running = false;
    }
  }
  
  private void execute(PreparedStatement statement) {
    long timer = System.currentTimeMillis();

    try {
      Util.debug("Executing the SQL task");
      statement.execute();
    } catch (SQLException sql) {
      QuickShop.instance().getSentryErrorReporter().ignoreThrow();
      sql.printStackTrace();
    }

    try {
      statement.close();
    } catch (SQLException sql) {
      QuickShop.instance().getSentryErrorReporter().ignoreThrow();
      sql.printStackTrace();
    }

    long took = System.currentTimeMillis() - timer;
    if (took > 1000)
      warningSender.sendWarn("Database performance warning: "
          + "It took too long time (" + took + "ms) to execute the task, "
          + "it may caused by the connection with database server or just database server too slow,"
          + "change to a better database server or switch to a local database instead!");
  }
  
  public void flush() {
    dispatcherThread.interrupt();
    ShopLogger.instance().info("Please wait for the data to flush...");
    sqlQueue.forEach(statement -> execute(statement));
    sqlQueue.clear();
    
    try {
      database.getConnector().close();
    } catch (Throwable t) {
      ;
    }
  }
}
