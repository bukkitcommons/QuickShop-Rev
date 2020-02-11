package cc.bukkit.shop;

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
import com.google.gson.JsonSyntaxException;
import cc.bukkit.shop.data.ShopCreator;
import cc.bukkit.shop.data.ShopData;
import cc.bukkit.shop.data.ShopLocation;
import cc.bukkit.shop.viewer.ShopViewer;

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
public interface ShopManager {
  void clear();
  
  void viewLoadedShops(@NotNull Consumer<Collection<ContainerShop>> con);

  void createShop(@NotNull ContainerShop shop, @NotNull ShopCreator info);
  
  boolean hasLoadedShopAt(@NotNull Location loc);
  
  void unload(@NotNull ContainerShop shop);
  
  @NotNull
  ContainerShop load(@NotNull World world, @NotNull ShopData data) throws JsonSyntaxException, InvalidConfigurationException;
  
  @NotNull
  ContainerShop load(@NotNull ContainerShop shop);
  
  @NotNull
  Map<String, Map<Long, Map<Long, ContainerShop>>> getLoadedShops();
  
  @NotNull
  ShopViewer getLoadedShopAt(@NotNull Block block);

  @NotNull
  ShopViewer getLoadedShopAt(@NotNull ShopLocation loc);
  
  @Nullable
  Map<Long, ContainerShop> getLoadedShopsInChunk(@NotNull Chunk c);

  @Nullable
  Map<Long, ContainerShop> getLoadedShopsInChunk(@NotNull String world, int chunkX, int chunkZ);
  
  @NotNull
  ShopViewer getLoadedShopAt(@NotNull Location loc);
  
  @NotNull
  ShopViewer getLoadedShopAt(@NotNull String world, int x, int y, int z);
  
  @NotNull
  ShopViewer getLoadedShopFrom(@Nullable Location loc);

  @NotNull
  ShopViewer getLoadedShopFrom(@NotNull Block block);
}
