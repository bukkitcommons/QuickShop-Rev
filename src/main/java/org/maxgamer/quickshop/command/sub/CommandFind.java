package org.maxgamer.quickshop.command.sub;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.command.QuickShopCommand;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.shop.QuickShopLoader;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.command.CommandProcesser;

import cc.bukkit.shop.data.ShopData;

public class CommandFind extends QuickShopCommand {

  @Override
  public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String[] cmdArg) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(MsgUtil.getMessage("Only player can run this command", sender));
      return;
    }

    if (cmdArg.length < 1) {
      sender.sendMessage(MsgUtil.getMessage("command.no-type-given", sender));
      return;
    }

    final StringBuilder sb = new StringBuilder(cmdArg[0]);

    for (int i = 1; i < cmdArg.length; i++) {
      sb.append(" ").append(cmdArg[i]);
    }

    final String lookFor = sb.toString().toLowerCase();
    final Player p = (Player) sender;
    final Location loc = p.getEyeLocation().clone();
    final double minDistance = BaseConfig.findDistance;
    double minDistanceSquared = minDistance * minDistance;
    final int chunkRadius = (int) minDistance / 16 + 1;
    ShopData closest = null;
    CompletableFuture<Chunk> future = new CompletableFuture<>();
    QuickShop.instance().getBukkitAPIWrapper().getChunkAt(loc.getWorld(), loc, future);
    final Chunk c;
    try {
      c = future.get();
    } catch (Exception asyncErr) {
      sender.sendMessage("Cannot execute the command, see console for details.");
      QuickShop.instance().getSentryErrorReporter().sendError(asyncErr, "Unknown errors");
      QuickShop.instance().getSentryErrorReporter().ignoreThrow();
      asyncErr.printStackTrace();
      return;
    }
    for (int x = -chunkRadius + c.getX(); x < chunkRadius + c.getX(); x++) {
      for (int z = -chunkRadius + c.getZ(); z < chunkRadius + c.getZ(); z++) {
        Chunk d = c.getWorld().getChunkAt(x, z);
        @NotNull Optional<Map<Long, ShopData>> inChunk = Shop.getLoader().getShopsInChunk(d);

        if (!inChunk.isPresent()) {
          continue;
        }

        try {
          for (ShopData shop : inChunk.get().values()) {
            if (!Util.getItemStackName(Util.deserialize(shop.item())).toLowerCase().contains(lookFor)) {
              continue;
            }

            if (shop.location().bukkit().distanceSquared(loc) >= minDistanceSquared) {
              continue;
            }

            closest = shop;
            minDistanceSquared = shop.location().bukkit().distanceSquared(loc);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    if (closest == null) {
      sender.sendMessage(MsgUtil.getMessage("no-nearby-shop", sender, cmdArg[0]));
      return;
    }

    final Location lookat = closest.location().bukkit().clone().add(0.5, 0.5, 0.5);
    // Hack fix to make /qs find not used by /back
    QuickShop.instance().getBukkitAPIWrapper().teleportEntity(p, Util.lookAt(loc, lookat).add(0, -1.62, 0),
        PlayerTeleportEvent.TeleportCause.UNKNOWN);
    p.sendMessage(MsgUtil.getMessage("nearby-shop-this-way", sender,
        "" + (int) Math.floor(Math.sqrt(minDistanceSquared))));
  }
}
