package org.maxgamer.quickshop.scheduler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.Util;
import lombok.NonNull;
import lombok.SneakyThrows;

public class AsyncLogWatcher implements Runnable {
  private Queue<String> logs = new ConcurrentLinkedQueue<>();
  private FileWriter logFileWriter = null;
  private PrintWriter pw;

  public AsyncLogWatcher(QuickShop plugin, File log) {
    try {
      if (!log.exists()) {
        log.createNewFile();
      }
      logFileWriter = new FileWriter(log, true);
      pw = new PrintWriter(logFileWriter);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      plugin.getLogger().severe("Log file was not found!");
    } catch (IOException e) {
      e.printStackTrace();
      plugin.getLogger().severe("Could not create the log file!");
    }
  }

  public void add(@NotNull String s) {
    logs.add(s);
  }

  @SneakyThrows
  public void close() {
    if (logFileWriter != null) {
      logFileWriter.flush();
      logFileWriter.close();
    }
  }

  public void log(@NonNull String log) {
    Date date = Calendar.getInstance().getTime();
    Timestamp time = new Timestamp(date.getTime());
    this.add("[" + time + "] " + log);
  }

  @Override
  public void run() {
    for (String log : logs) {
      if (logFileWriter == null) {
        continue;
      }
      if (pw == null) {
        continue;
      }
      pw.println(log);
    }
    logs.clear();
    if (logFileWriter != null) {
      try {
        if (pw != null) {
          pw.flush();
        }
        logFileWriter.flush();
      } catch (IOException ioe) {
        Util.debug("Failed to flush log to disk: " + ioe.getMessage());
      }
    }
  }
}
