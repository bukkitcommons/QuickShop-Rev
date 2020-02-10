package cc.bukkit.shop.configuration;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.messages.ShopPluginLogger;
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
  
  public void upgrade(Class<?> confClass) {
    ConfigurationData data = configurations.get(confClass);
    if (data == null)
      return;
    
    // Backup that file with increased id
    int backupId = 1;
    File file = new File(data.file().getParentFile(), data.file().getName().concat("backup"));
    while (file.exists())
      file = new File(file.getParentFile(), file.getName() + backupId++);
    
    forEachNode(confClass, (field, node) -> {
      NodeType type = node.type();
      switch (type) {
        case CONFIG:
          return;
        case REMOVE:
          data.conf().set(node.value(), null);
          break;
        case MOVE:
          YamlConfiguration conf = data.conf();
          if (StringUtils.contains(node.ref(), '/')) {
            // Across files action
            String[] params = StringUtils.split(node.ref(), '/');
            File refFile = new File(QuickShop.instance().getDataFolder(), params[0]);
            if (params.length > 2) {
              refFile = new File(refFile, params[1]); // Nested folder
              params[1] = params[2];
            }
            
            if (!refFile.exists())
              return;
            
            YamlConfiguration ref = YamlConfiguration.loadConfiguration(refFile);
            if (ref.isSet(params[1]))
              conf.set(node.value(), ref.get(params[1]));
          } else {
            if (conf.isSet(node.ref()))
              conf.set(node.value(), conf.get(node.ref()));
          }
          break;
      }
    });
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
    Util.debug("Loading configuration class: " + confClass.getName());
    
    if (filePath != null) {
      Util.debug("Path of configuration " + confClass.getName() + " is " + filePath.value());
      File file = deploysConfigurationFile(new File(parent, filePath.value()), filePath.value());
      YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);

      ConfigurationData data = new ConfigurationData(conf, file);
      configurations.put(confClass, data);
      
      Util.debug("Upgrading config file: " + filePath.value());
      upgrade(confClass);

      Util.debug("Loading config file: " + filePath.value());
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
