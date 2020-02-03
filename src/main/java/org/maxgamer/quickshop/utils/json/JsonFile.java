package org.maxgamer.quickshop.utils.json;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.ToString;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.Rewriter;

@ToString
public class JsonFile implements LocaleFile {
  @NotNull
  protected final File file;
  @NotNull
  private final Rewriter rewriter;
  @NotNull
  private final String resourcePath;
  
  @NotNull
  protected FileConfiguration fileConfiguration = new FileConfiguration() {
    @Override
    public String saveToString() {
      return "";
    }
    
    @Override
    public void loadFromString(String contents) throws InvalidConfigurationException {
      ;
    }
    
    @Override
    protected String buildHeader() {
      return "";
    }
  };

  public JsonFile(@NotNull File file, @NotNull String resourcePath) {
    this.file = file;
    this.rewriter = new Rewriter(file);
    this.resourcePath = resourcePath;
  }

  @Override
  public void create() {
    if (!file.exists()) {
      try {
        final File parent = file.getParentFile();

        if (parent != null) {
          parent.mkdirs();
        }

        file.createNewFile();
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }

      rewriter.accept(getInputStream());
    }

    setDefaults();
  }
  
  @Override
  public void setDefaults() {
    fileConfiguration = JSONConfiguration.loadConfiguration(file);
    fileConfiguration.setDefaults(JSONConfiguration.loadConfiguration(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)));
  }

  @NotNull
  @Override
  public InputStream getInputStream() {
    return QuickShop.instance().getResource(resourcePath);
  }

  @Override
  public void save() {
    try {
      fileConfiguration.save(file);
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  @NotNull
  @Override
  public String saveToString() {
    return fileConfiguration.saveToString();
  }

  @Override
  public void loadFromString(@NotNull String data) throws InvalidConfigurationException {
    fileConfiguration.loadFromString(data);
  }

  @Nullable
  @Override
  public Object get(@NotNull String path) {
    return fileConfiguration.get(path);
  }

  @Nullable
  @Override
  public Object get(@NotNull String path, @Nullable Object defaultValue) {
   return fileConfiguration.get(path, defaultValue);
  }

  @NotNull
  @Override
  public Optional<String> getString(@NotNull String path) {
    return Optional.ofNullable(fileConfiguration.getString(path));
  }

  @Override
  public void set(@NotNull String path, @Nullable Object object) {
    fileConfiguration.set(path, object);
    save();
  }

  @NotNull
  @Override
  public List<String> getStringList(@NotNull String path) {
    return fileConfiguration.getStringList(path);
  }
}
