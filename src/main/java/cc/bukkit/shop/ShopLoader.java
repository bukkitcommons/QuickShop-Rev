package cc.bukkit.shop;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;
import cc.bukkit.shop.data.ShopData;

public interface ShopLoader {
  void loadShops();
  
  @NotNull
  List<ShopData> getAllShops();
  
  void forEachShops(@NotNull Consumer<ShopData> consumer);
  
  @NotNull
  Optional<Map<Long, ShopData>> getShopsInChunk(@NotNull Chunk c);

  @NotNull
  Optional<Map<Long, ShopData>> getShopsInChunk(@NotNull String world, int chunkX, int chunkZ);

  @NotNull
  Optional<Map<Long, Map<Long, ShopData>>> getShopsInWorld(@NotNull String world);

  void delete(@NotNull ShopData shop) throws SQLException;

  void delete(ContainerShop shop);

  Map<String, Map<Long, Map<Long, ShopData>>> getShopsMap();
}
