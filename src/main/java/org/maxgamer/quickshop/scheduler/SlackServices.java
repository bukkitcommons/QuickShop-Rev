package org.maxgamer.quickshop.scheduler;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SlackServices implements Runnable {
    @SuppressWarnings("rawtypes")
    private static final Map<Object, Queue<Consumer>> tasks = Maps.newHashMap();
    
    public static <T> void schedule(@NotNull T object, Consumer<T> consumer) {
        tasks.computeIfAbsent(object, o -> Lists.newLinkedList()).add(consumer);
    }
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void run() {
        Iterator<Entry<Object, Queue<Consumer>>> iterator = tasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Object, Queue<Consumer>> entry = iterator.next();
            
            Queue<Consumer> tasks = entry.getValue();
            while (!tasks.isEmpty())
                entry.getValue().poll().accept(entry.getKey());
            
            iterator.remove();
        }
    }
}
