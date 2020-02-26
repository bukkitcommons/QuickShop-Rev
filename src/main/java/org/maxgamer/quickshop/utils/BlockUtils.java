package org.maxgamer.quickshop.utils;

import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockUtils {
    
    /**
     * Fetches the block which the given sign is attached to
     *
     * @param sign The block which is attached
     * @return The block the sign is attached to
     */
    public static Optional<Block> getSignAttached(@NotNull Block sign) {
        if (!BlockUtils.isWallSign(sign.getType()))
            return Optional.empty();
        
        try {
            org.bukkit.block.data.BlockData data = sign.getBlockData();
            if (data instanceof org.bukkit.block.data.type.WallSign) {
                BlockFace opposide = ((org.bukkit.block.data.type.WallSign) data).getFacing().getOppositeFace();
                return Optional.of(sign.getRelative(opposide));
            }
        } catch (Throwable t) {
            return Optional.of(sign.getRelative(((org.bukkit.material.Sign) sign.getState().getData()).getFacing().getOppositeFace()));
        }
        
        return Optional.empty();
    }
    
    public static boolean isAir(@NotNull Material mat) {
        if (mat == Material.AIR) {
            return true;
        }
        /* For 1.13 new AIR */
        try {
            if (mat == Material.CAVE_AIR) {
                return true;
            }
            if (mat == Material.VOID_AIR) {
                return true;
            }
        } catch (Throwable t) {
            // ignore
        }
        return false;
    }
    
    public static Location getCenter(@NotNull Location location) {
        // This is always '+' instead of '-' even in negative pos
        return location.clone().add(.5, .5, .5);
    }
    
    public static final BlockFace[] axis = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    
    /**
     * Gets the horizontal Block Face from a given yaw angle<br>
     * This includes the NORTH_WEST faces
     *
     * @param yaw angle
     * @return The Block Face of the angle
     */
    public static BlockFace yawToFace(float yaw) {
        return axis[Math.round(yaw / 90f) & 0x3];
    }
    
    /**
     * Returns the chest attached to the given chest. The given block must be a
     * chest.
     *
     * @param chest The chest to check.
     * @return the block which is also a chest and connected to b.
     */
    public static Optional<Location> getSecondHalf(@NotNull Block chest) {
        if (!isDoubleChest(chest))
            return Optional.empty();
        
        Chest halfChest = (Chest) chest.getState();
        DoubleChestInventory chestHolder = (@Nullable DoubleChestInventory) halfChest.getInventory();
        
        Inventory right = chestHolder.getRightSide();
        Location rightLoc = right.getLocation();
        return Optional.of((rightLoc.getX() == halfChest.getX() && rightLoc.getZ() == halfChest.getZ() ? chestHolder.getLeftSide().getLocation() : rightLoc));
    }
    
    public static Location deserializeLocation(@NotNull String location) {
        Util.trace("will deserilize: " + location);
        String[] sections = location.split(",");
        String worldName = StringUtils.substringBetween(sections[0], "{name=", "}");
        String x = sections[1].substring(2);
        String y = sections[2].substring(2);
        String z = sections[3].substring(2);
        
        return new Location(Bukkit.getWorld(worldName), Double.valueOf(x), Double.valueOf(y), Double.valueOf(z));
    }
    
    public static boolean hasSpaceForDisplay(@NotNull Material mat) {
        return isAir(mat) || isWallSign(mat);
    }
    
    public static boolean isDoubleChest(@Nullable Block b) {
        if (b == null) {
            return false;
        }
        if (!(b.getState() instanceof Container)) {
            return false;
        }
        Container container = (Container) b.getState();
        return (container.getInventory() instanceof DoubleChestInventory);
    }
    
    /**
     * Check a material is or not a WALL_SIGN
     *
     * @param material mat
     * @return is or not a wall_sign
     */
    public static boolean isWallSign(@Nullable Material material) {
        if (material == null) {
            return false;
        }
        try {
            return BlockDataWrapper.isWallSigns(material);
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            return "WALL_SIGN".equals(material.name());
        }
    }
    
    private static class BlockDataWrapper {
        private static boolean isWallSigns(Material material) throws NoClassDefFoundError, ClassNotFoundException {
            return org.bukkit.Tag.WALL_SIGNS.isTagged(material);
        }
    }
}
