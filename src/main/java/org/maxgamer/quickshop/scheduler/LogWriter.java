package org.maxgamer.quickshop.scheduler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.concurrent.BlockingQueue;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.Queues;
import lombok.NonNull;
import lombok.SneakyThrows;

public class LogWriter implements Runnable {
    @NotNull
    private final BlockingQueue<String> loggings = Queues.newLinkedBlockingQueue();
    @NotNull
    private final FileWriter fileWriter;
    @NotNull
    private final PrintWriter printWriter;
    
    private volatile boolean running = true;
    private final Timestamp stamp = new Timestamp(System.currentTimeMillis());
    
    public LogWriter(@NotNull File file) throws IOException {
        file.createNewFile();
        fileWriter = new FileWriter(file);
        printWriter = new PrintWriter(fileWriter);
    }
    
    @SneakyThrows
    public void close() {
        running = false;
        
        loggings.forEach(message -> printWriter.print(message));
        loggings.clear();
        
        printWriter.flush();
        fileWriter.flush();
        
        printWriter.close();
        fileWriter.close();
    }
    
    public void log(@NonNull String message) {
        stamp.setTime(System.currentTimeMillis());
        loggings.add(stamp + " > " + message);
    }
    
    @Override
    public void run() {
        try {
            while (running)
                printWriter.println(loggings.take());
        } catch (InterruptedException interrupted) {
            running = false;
        }
    }
}
