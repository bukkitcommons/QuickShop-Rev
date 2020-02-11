package cc.bukkit.shop.data;

import java.io.Serializable;
import javax.annotation.concurrent.Immutable;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import cc.bukkit.shop.util.Utils;
import lombok.Getter;
import lombok.experimental.Accessors;

@Immutable
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
  
  @Getter
  private final long blockKey;
  
  public ShopLocation(@NotNull String world, int x, int y, int z) {
    this.worldName = world;
    this.x = x;
    this.y = y;
    this.z = z;
    this.blockKey = Utils.blockKey(x, y, z);
  }
  
  public static ShopLocation from(@NotNull String world, int x, int y, int z) {
    return new ShopLocation(world, x, y, z);
  }
  
  public static ShopLocation from(@NotNull World world, int x, int y, int z) {
    return new ShopLocation(world, x, y, z);
  }
  
  @NotNull
  @Override
  public ShopLocation clone() {
    return new ShopLocation(this);
  }
  
  public ShopLocation(@NotNull ShopLocation location) {
    worldName = location.worldName;
    world = location.world;
    x = location.x;
    y = location.y;
    z = location.z;
    blockKey = location.blockKey;
  }
  
  public ShopLocation(@NotNull World world, int x, int y, int z) {
    this.world = world;
    this.worldName = world.getName();
    this.x = x;
    this.y = y;
    this.z = z;
    this.blockKey = Utils.blockKey(x, y, z);
  }
  
  public static ShopLocation of(@NotNull Location location) {
    return new ShopLocation(location);
  }
  
  public ShopLocation(@NotNull Location location) {
    world = location.getWorld();
    // This will from events and player commands,
    // in where the world won't be null.
    assert world != null;
    worldName = world.getName();
    
    x = location.getBlockX();
    y = location.getBlockY();
    z = location.getBlockZ();
    
    bukkit = location;
    blockKey = Utils.blockKey(x, y, z);
  }
  
  /*
   * Transient members with cache
   */
  @NotNull
  private transient World world;
  
  /**
   * This only guarantee that it will not be null
   * on the aspect of plugin design.
   * @return the world this location in
   */
  @NotNull
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
  
  @Override
  public String toString() {
    return bukkit().toString();
  }
}
