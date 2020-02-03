package org.maxgamer.quickshop.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.Util;
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
  private int taskId;

  /**
   * Queued database manager. Use queue to solve run SQL make server lagg issue.
   *
   * @param plugin plugin main class
   * @param db database
   */
  public Dispatcher(@NotNull Database db) {
    warningSender = new WarningSender(QuickShop.instance(), 600000);
    database = db;
    taskId =
        Bukkit.getScheduler()
              .runTaskTimerAsynchronously(QuickShop.instance(), this, 0, 10 * 20).getTaskId();
  }

  /**
   * Add preparedStatement to queue waiting flush to database,
   *
   * @param ps The ps you want add in queue.
   */
  public void add(@NotNull PreparedStatement ps) {
    sqlQueue.offer(ps);
  }

  @Override
  public void run() {
    try {
      while (true)
        execute(sqlQueue.take());
    } catch (InterruptedException interrupted) {
      run();
    }
  }
  
  private void execute(PreparedStatement statement) {
    long timer = System.currentTimeMillis();

    try {
      Util.debugLog("Executing the SQL task: " + statement);
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
    if (took > 5000)
      warningSender.sendWarn("Database performance warning: "
          + "It took too long time (" + took + "ms) to execute the task, "
          + "it may caused by the connection with database server or just database server too slow,"
          + "change to a better database server or switch to a local database instead!");
  }
  
  public void flush() {
    Bukkit.getScheduler().cancelTask(taskId);
    QuickShop.instance().getLogger().info("Please wait for the data to flush its data...");
    sqlQueue.forEach(statement -> execute(statement));
  }
}
