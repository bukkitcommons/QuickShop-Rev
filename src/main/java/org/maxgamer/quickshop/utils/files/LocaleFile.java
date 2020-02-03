package org.maxgamer.quickshop.utils.files;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LocaleFile {

  /** Creates yml file on the path */
  void create();

  @NotNull
  InputStream getInputStream();

  /** Reloads file */
  void setDefaults();

  /** Saves file */
  void save();

  /** @return the string of the file */
  @NotNull
  String saveToString();

  /**
   * Loads from the data
   *
   * @param data the data
   */
  void loadFromString(@NotNull String data) throws InvalidConfigurationException;

  /**
   * Gets the object
   *
   * @param path object path to get
   * @return if path does not exist returns null
   */
  @Nullable
  Object get(@NotNull String path);

  /**
   * Gets the object with fallback
   *
   * @param path object path to get
   * @param fallback fallback object to get if path does not exist
   * @return if path does not exist returns fallback object
   */
  @Nullable
  Object get(@NotNull String path, @Nullable Object fallback);

  /**
   * Gets string
   *
   * @param path string path to get
   * @return {@link String}
   */
  @NotNull
  Optional<String> getString(@NotNull final String path);

  /**
   * Sets object to path
   *
   * @param path object path to set
   * @param object {@link Object}
   */
  void set(@NotNull final String path, @Nullable final Object object);

  /**
   * Gets string list
   *
   * @param path string list path to get
   * @return string list
   */
  @NotNull
  List<String> getStringList(@NotNull final String path);
}
