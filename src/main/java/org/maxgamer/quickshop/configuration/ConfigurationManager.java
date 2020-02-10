package org.maxgamer.quickshop.configuration;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
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
        if (node.rewrite()) {
          Util.debug(field.getName() + " rewritted.");
          data.conf().set(path, getStatic(field));
        }
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
    Util.debug("Loading configuration for " + confClass.getName());
    
    if (filePath != null) {
      Util.debug("Path of configuration " + confClass.getName() + " is " + filePath.value());
      File file = deploysConfigurationFile(new File(parent, filePath.value()), filePath.value());
      YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);

      ConfigurationData data = new ConfigurationData(conf, file);
      configurations.put(confClass, data);

      forEachNode(confClass, (field, node) -> {
        String path = node.value();
        Object value = conf.get(path);
        
        if (value == null) {
          Util.debug(field.getName() + ": " + value);
          value = getStatic(field);
          
          conf.set(path, value);
        } else {
          setStatic(field, value);
        }
      });

      conf.save(file);
      return data;
    }
    return null;
  }

  private File deploysConfigurationFile(@NotNull File file, @NotNull String path) {
    createParents(file);
    if (file.exists()) return file;
    
    try {
      InputStream jarFile = QuickShop.instance().getResource(path);
      
      if (jarFile == null) {
        file.createNewFile();
      } else {
        Util.debug("Copying resource from Jar to " + path);
        java.nio.file.Files.copy(jarFile, file.toPath());
      }
    } catch (Throwable t) {
      ShopLogger.instance().severe("Failed to prepare configuration file, corrupted jar? @ " + path);
      t.printStackTrace();
    }
    
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
        Util.debug("Making dir for parent: " + parent.getCanonicalPath());
        parent.mkdir();
      }
    }
  }
}
