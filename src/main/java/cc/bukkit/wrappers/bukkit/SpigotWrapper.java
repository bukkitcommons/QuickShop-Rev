package cc.bukkit.wrappers.bukkit;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpigotWrapper implements BukkitWrapper {
  @Override
  public void teleportEntity(
      @NotNull Entity entity,
      @NotNull Location location,
      @Nullable PlayerTeleportEvent.TeleportCause cause) {
    
    if (cause == null)
      entity.teleport(location);
    else
      entity.teleport(location, cause);
  }

  @Override
  public void getChunkAt(
      @NotNull World world,
      @NotNull Location location,
      @NotNull CompletableFuture<Chunk> futureTask) {
    
    futureTask.complete(world.getChunkAt(location));
  }

  @Override
  public void getChunkAt(
      @NotNull World world,
      int x, int z,
      @NotNull CompletableFuture<Chunk> futureTask) {
    
    futureTask.complete(world.getChunkAt(x, z));
  }

  @Override
  public void getChunkAt(
      @NotNull World world,
      @NotNull Block block,
      @NotNull CompletableFuture<Chunk> futureTask) {
    
    futureTask.complete(world.getChunkAt(block));
  }
}
