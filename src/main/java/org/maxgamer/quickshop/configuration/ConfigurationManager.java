package org.maxgamer.quickshop.configuration;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

@EqualsAndHashCode
@Accessors(fluent = true)
@RequiredArgsConstructor
public class ConfigurationManager {
  private final File parent;
  private final Map<Class<?>, ConfigurationData> configurations = Maps.newHashMap();
  
  public ConfigurationManager unload(@NotNull Class<?> confClass) {
    configurations.remove(confClass);
    return this;
  }
  
  @Nullable
  public ConfigurationData get(@NotNull Class<?> confClass) {
    return configurations.get(confClass);
  }

  @SneakyThrows
  public ConfigurationData save(@NotNull Class<?> confClass) {
    ConfigurationData data = configurations.get(confClass);
    if (data != null) {
      forEachNode(confClass, (field, node) -> {
        String path = node.value();
        Util.debugLog("rewrite: " + field.getName() + ", value " + data.conf().get(path));
        if (node.rewrite()) {
          data.conf().set(path, getStatic(field));
        }
        Util.debugLog("rewrite: " + field.getName() + ", value (fixed) " + getStatic(field));
      });

      data.conf().save(data.file());
    }
    return data;
  }

  @SneakyThrows
  private static Object getStatic(@NotNull Field field) {
    return field.get(null);
  }

  @SneakyThrows
  private static void setStatic(@NotNull Field field, @Nullable Object value) {
    field.set(null, value);
  }

  public static void forEachNode(Class<?> confClass, BiConsumer<Field, Node> consumer) {
    Field[] fields = confClass.getDeclaredFields();
    for (Field field : fields) {
      Node node = field.getAnnotation(Node.class);
      if (node != null) {
        consumer.accept(field, node);
      }
    }
  }

  @NotNull
  public static ConfigurationManager createManager(@NotNull Plugin plugin) {
    return new ConfigurationManager(new File(".".concat(File.separator).concat("plugins")
        .concat(File.separator).concat(plugin.getName()).concat(File.separator)));
  }

  @Nullable
  @SneakyThrows
  public ConfigurationData load(@NotNull Class<?> confClass) {
    Configuration filePath = confClass.getAnnotation(Configuration.class);
    Util.debugLog("Loading configuration for " + confClass.getName());
    
    if (filePath != null) {
      Util.debugLog("Path of configuration " + confClass.getName() + " is " + filePath.value());
      File file = createConfigurationFile(new File(parent, filePath.value()));
      YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);

      ConfigurationData data = new ConfigurationData(conf, file);
      configurations.put(confClass, data);

      forEachNode(confClass, (field, node) -> {
        String path = node.value();
        Object value = conf.get(path);
        Util.debugLog("field: " + field.getName() + ", value " + value);
        if (value == null) {
          value = getStatic(field);
        } else {
          setStatic(field, value);
        }
        Util.debugLog("field: " + field.getName() + ", value (fixed) " + value);
      });

      return data;
    }
    return null;
  }

  @SneakyThrows
  private File createConfigurationFile(@NotNull File file) {
    createParents(file);
    file.createNewFile();
    return file;
  }

  @SneakyThrows
  private void createParents(@NotNull File file) {
    List<File> parents = Lists.newArrayList();

    synchronized (parent) {
      while ((file = file.getParentFile()) != null) {
        parents.add(file);
      }

      for (File parent : Lists.reverse(parents)) {
        Util.debugLog("Making dir for parent: " + parent.getCanonicalPath());
        parent.mkdir();
      }
    }
  }
}
