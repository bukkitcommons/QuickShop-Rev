package org.maxgamer.quickshop.utils.wrappers.bukkit.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.utils.wrappers.bukkit.BukkitWrapper;

public class PaperWrapper implements BukkitWrapper {
  @Override
  public void teleportEntity(
      @NotNull Entity entity,
      @NotNull Location location,
      @Nullable PlayerTeleportEvent.TeleportCause cause) {
    
    if (cause == null)
      entity.teleportAsync(location);
    else
      entity.teleportAsync(location, cause);
  }

  @Override
  public void getChunkAt(
      @NotNull World world,
      @NotNull Location location,
      @NotNull CompletableFuture<Chunk> futureTask) {
    
    try {
      futureTask.complete(world.getChunkAtAsync(location).get());
    } catch (InterruptedException | ExecutionException e) {
      futureTask.complete(world.getChunkAt(location));
    }
  }

  @Override
  public void getChunkAt(
      @NotNull World world,
      int x, int z,
      @NotNull CompletableFuture<Chunk> futureTask) {
    
    try {
      futureTask.complete(world.getChunkAtAsync(x, z).get());
    } catch (InterruptedException | ExecutionException e) {
      futureTask.complete(world.getChunkAt(x, z));
    }
  }

  @Override
  public void getChunkAt(
      @NotNull World world,
      @NotNull Block block,
      @NotNull CompletableFuture<Chunk> futureTask) {
    
    try {
      futureTask.complete(world.getChunkAtAsync(block).get());
    } catch (InterruptedException | ExecutionException e) {
      futureTask.complete(world.getChunkAt(block));
    }
  }
}
