package org.maxgamer.quickshop.utils.viewer;

import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

public class BlockViewer {
  private final Location from;
  private final int to;
  
  public BlockViewer(Location origin, int distance) {
    from = origin  ;
    to   = distance;
    iterator = new BlockIterator(from, to);
  }
  
  private BlockIterator iterator;
  private boolean none;
  
  public static BlockViewer get(Player player, int to) {
    return new BlockViewer(player.getLocation(), to);
  }
  
  public static BlockViewer get(Entity entity, int to) {
    return new BlockViewer(entity.getLocation(), to);
  }
  
  public static BlockViewer get(Location at, int to) {
    return new BlockViewer(at, to);
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
