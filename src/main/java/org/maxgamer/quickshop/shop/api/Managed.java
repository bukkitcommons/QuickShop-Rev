package org.maxgamer.quickshop.shop.api;

import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface Managed {
  public void setOwner(@NotNull UUID uuid);

  public boolean addStaff(@NotNull UUID player);

  public void clearStaffs();

  public boolean removeStaff(@NotNull UUID player);

  public boolean isModerator(@NotNull UUID player);

  public boolean isOwner(@NotNull UUID player);

  public boolean isStaff(@NotNull UUID player);
  
  @NotNull public UUID getOwner();

  @NotNull public Set<UUID> getStaffs();
}
