package org.maxgamer.quickshop.shop.api.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Accessors(fluent = true)
public class ShopLocation {
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
  
  private World world;
  
  public World world() {
    return world == null ? (world = Bukkit.getWorld(worldName)) : world;
  }
  
  private Chunk chunk;
  
  public Chunk chunk() {
    return chunk == null ? (chunk = world().getChunkAt(x >> 4, z >> 4)) : chunk;
  }
  
  private Block block;
  
  public Block block() {
    return block == null ? (block = world().getBlockAt(x, y, z)) : block;
  }
  
  private Location bukkit;
  
  public Location bukkit() {
    return bukkit == null ? (bukkit = new Location(world(), x, y, z)) : bukkit;
  }
}
