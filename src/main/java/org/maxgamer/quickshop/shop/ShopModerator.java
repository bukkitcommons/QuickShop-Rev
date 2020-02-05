package org.maxgamer.quickshop.shop;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * Contains shop's moderators infomations, owner, staffs etc.
 */
@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class ShopModerator {
  private static final Gson GSON = new Gson();
  
  private UUID owner;
  private final Set<UUID> staffs = Sets.newHashSet();

  public static ShopModerator deserialize(@NotNull String serilized) throws JsonSyntaxException {
    return GSON.fromJson(serilized, ShopModerator.class);
  }
  
  public void setOwner(@NotNull UUID uuid) {
    owner = uuid;
  }

  public String serialize() {
    return GSON.toJson(this);
  }

  /**
   * Add moderators staff to staff list
   *
   * @param player New staff
   * @return Success
   */
  public boolean addStaff(@NotNull UUID player) {
    return staffs.add(player);
  }

  /**
   * Remove all staffs
   */
  public void clearStaffs() {
    staffs.clear();
  }

  @Override
  @NotNull
  public ShopModerator clone() {
    ShopModerator copy = new ShopModerator(owner);
    copy.staffs.addAll(staffs);
    return copy;
  }

  @Override
  @NotNull
  public String toString() {
    return serialize();
  }

  /**
   * Remove moderators staff from staff list
   *
   * @param player Staff
   * @return Success
   */
  public boolean removeStaff(@NotNull UUID player) {
    return staffs.remove(player);
  }

  /**
   * Get a player is or not moderators
   *
   * @param player Player
   * @return yes or no, return true when it is staff or owner
   */
  public boolean isModerator(@NotNull UUID player) {
    return isOwner(player) || isStaff(player);
  }

  /**
   * Get a player is or not moderators owner
   *
   * @param player Player
   * @return yes or no
   */
  public boolean isOwner(@NotNull UUID player) {
    return player.equals(owner);
  }

  /**
   * Get a player is or not moderators a staff
   *
   * @param player Player
   * @return yes or no
   */
  public boolean isStaff(@NotNull UUID player) {
    return staffs.contains(player);
  }
}
