package org.maxgamer.quickshop.shop.api.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import java.io.Serializable;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
@Accessors(fluent = true)
public class ShopLocation implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Getter
  @NotNull
  private final String worldName;
  
  @Getter
  private final int x;
  @Getter
  private final int y;
  @Getter
  private final int z;
  
  public ShopLocation(@NotNull Location location) {
    world = location.getWorld();
    worldName = world.getName();
    
    x = location.getBlockX();
    y = location.getBlockY();
    z = location.getBlockZ();
    
    bukkit = location;
  }
  
  /*
   * Transient member with cache
   */
  @Nullable
  private transient World world;
  
  @Nullable
  public World world() {
    return world == null ? (world = Bukkit.getWorld(worldName)) : world;
  }
  
  @Nullable
  private transient Chunk chunk;
  
  @NotNull
  public Chunk chunk() {
    return chunk == null ? (chunk = world().getChunkAt(x >> 4, z >> 4)) : chunk;
  }
  
  @Nullable
  private transient Block block;
  
  @NotNull
  public Block block() {
    return block == null ? (block = world().getBlockAt(x, y, z)) : block;
  }
  
  @Nullable
  private transient Location bukkit;
  
  @NotNull
  public Location bukkit() {
    return bukkit == null ? (bukkit = new Location(world(), x, y, z)) : bukkit;
  }
}
