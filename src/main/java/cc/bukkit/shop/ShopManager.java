package cc.bukkit.shop;

import com.google.gson.JsonSyntaxException;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.data.ShopCreator;
import cc.bukkit.shop.data.ShopData;
import cc.bukkit.shop.data.ShopLocation;
import cc.bukkit.shop.viewer.ShopViewer;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This manage all loaded shops, holding them, capable of
 * loading shops by its data, or unload a loaded shop.
 * Also provides ability to get the instance of loaded shop(s), by block, chunk, etc.<br>
 * 
 * <br>Briefly, the core of loaded shops, to get, to load and unload.<br>
 * 
 * <br>For shop loading operation, a shop instance of its plain data is needed.<br>
 * 
 * <br>For shop getting, a viewer is return as a solution for null.
 * The viewer is literally a wrapper of Java Optional for better accessing.
 * 
 * @see ShopData
 * @see ShopViewer
 * @see Optional
 */
interface ShopManager {
  /**
   * Adds a shop to the world. Does NOT require the chunk or world to be loaded Call shop.onLoad by
   * yourself
   *
   * @param world The name of the world
   * @param shop The shop to add
   * @throws InvalidConfigurationException 
   * @throws JsonSyntaxException 
   */
  Shop load(@NotNull World world, @NotNull ShopData data) throws JsonSyntaxException, InvalidConfigurationException;
  
  Shop load(@NotNull Shop shop);

  /**
   * Removes all shops from memory and the world. Does not delete them from the database. Call this
   * on plugin disable ONLY.
   */
  void clear();
  
  void viewLoadedShops(Consumer<Collection<Shop>> con);

  /**
   * Create a shop use Shop and Info object.
   *
   * @param shop The shop object
   * @param info The info object
   */
  void createShop(@NotNull Shop shop, @NotNull ShopCreator info);
  
  ShopViewer getLoadedShopAt(@NotNull Block block);

  /**
   * Gets a shop in a specific location
   *
   * @param loc The location to get the shop from
   * @return The shop at that location
   */
  ShopViewer getLoadedShopAt(@NotNull ShopLocation loc);
  
  @Nullable
  Map<Long, Shop> getLoadedShopsInChunk(@NotNull Chunk c);

  @Nullable Map<Long, Shop> getLoadedShopsInChunk(@NotNull String world, int chunkX, int chunkZ);
  
  ShopViewer getLoadedShopAt(Location loc);
  
  ShopViewer getLoadedShopAt(String world, int x, int y, int z);
  
  boolean hasLoadedShopAt(@NotNull Location loc);
  
  /**
   * Gets a shop in a specific location Include the attached shop, e.g DoubleChest shop.
   *
   * @param loc The location to get the shop from
   * @return The shop at that location
   */
  ShopViewer getLoadedShopFrom(@Nullable Location loc);

  ShopViewer getLoadedShopFrom(@NotNull Block block);

  /**
   * Removes a shop from the world. Does NOT remove it from the database.
   *
   * @param shop The shop to remove
   */
  void unload(@NotNull Shop shop);

  /**
   * Get all loaded shops.
   *
   * @return All loaded shops.
   */
  @NotNull
  Map<String, Map<Long, Map<Long, Shop>>> getLoadedShops();
}
