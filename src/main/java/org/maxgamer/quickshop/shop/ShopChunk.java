package org.maxgamer.quickshop.shop;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ShopChunk {
  private final String world;
  private final int chunkX;
  private final int chunkZ;
}
