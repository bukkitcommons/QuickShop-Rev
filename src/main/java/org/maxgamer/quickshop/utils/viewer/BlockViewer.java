package org.maxgamer.quickshop.utils.viewer;

import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockIterator;

public class BlockViewer {
  private final Location from;
  private final int to;
  
  public BlockViewer(Location origin, double offset, int distance) {
    from = origin  ;
    to   = distance;
    iterator = new BlockIterator(from, offset, to);
  }
  
  private BlockIterator iterator;
  private boolean none;
  
  public static BlockViewer get(LivingEntity entity, int to) {
    return new BlockViewer(entity.getLocation(), entity.getEyeHeight(), to);
  }
  
  public static BlockViewer get(Location eye, int to) {
    return new BlockViewer(eye, 0, to);
  }
  
  public BlockViewer ifEmpty(Runnable runnable) {
    if (!iterator.hasNext())
      runnable.run();
    return this;
  }
  
  public BlockViewer forEach(Function<Block, ViewAction> function) {
    while (iterator.hasNext()) {
      ViewAction action = function.apply(iterator.next());
      
      switch (action) {
        case NEXT:
          continue;
        case BREAK:
          return this;
      }
    }
    
    none = true;
    return this;
  }
  
  public <R> R ifNone(Supplier<R> function) {
    if (none)
      return function.get();
    return null;
  }
  
  public <R> R ifNone(Supplier<R> function, R defaultValue) {
    if (none)
      return function.get();
    return defaultValue;
  }
  
  public void ifNone(Runnable runnable) {
    if (none)
      runnable.run();
  }
}
