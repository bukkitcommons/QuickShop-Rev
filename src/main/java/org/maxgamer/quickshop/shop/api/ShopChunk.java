package org.maxgamer.quickshop.shop.api;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ShopChunk {
  private final String world;
  private final int chunkX;
  private final int chunkZ;
}
