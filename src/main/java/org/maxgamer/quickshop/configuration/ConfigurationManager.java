package org.maxgamer.quickshop.configuration;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.configuration.annotation.Configuration;
import org.maxgamer.quickshop.configuration.annotation.Node;
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
  
  @SneakyThrows
  public ConfigurationData save(Class<?> confClass) {
    ConfigurationData data = configurations.get(confClass);
    
    forEachNode(confClass, (field, node) -> {
      String path = node.value();
      Bukkit.getLogger().warning("rewrite: " + field.getName() + ", value " + data.conf().get(path));
      if (node.rewrite()) {
        data.conf().set(path, getStatic(field));
      }
      Bukkit.getLogger().warning("rewrite: " + field.getName() + ", value (fixed) " + getStatic(field));
    });
    
    data.conf().save(data.file());
    return data;
  }
  
  @SneakyThrows
  public static Object getStatic(Field field) {
    return field.get(null);
  }
  
  @SneakyThrows
  public static void setStatic(Field field, Object value) {
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
  
  public static ConfigurationManager getManager(@NotNull Plugin plugin) {
    return new ConfigurationManager(new File(
        ".".concat(File.separator)
           .concat("plugins")
           .concat(File.separator)
           .concat(plugin.getName())
           .concat(File.separator)
           ));
  }
  
  @SneakyThrows
  public void load(Class<?> confClass) {
    Configuration filePath = confClass.getAnnotation(Configuration.class);
    if (filePath != null) {
      File file = createConfigurationFile(new File(parent, filePath.value()));
      YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);
      
      ConfigurationData data = new ConfigurationData(conf, file);
      configurations.put(confClass, data);
      
      forEachNode(confClass, (field, node) -> {
        String path = node.value();
        Object value = conf.get(path);
        Bukkit.getLogger().warning("field: " + field.getName() + ", value " + value);
        if (value == null) {
          value = getStatic(field);
        } else {
          setStatic(field, value);
        }
        Bukkit.getLogger().warning("field: " + field.getName() + ", value (fixed) " + value);
      });
      
    }
  }
  
  @SneakyThrows
  private File createConfigurationFile(File file) {
    createParents(file);
    file.createNewFile();
    return file;
  }
  
  @SneakyThrows
  private void createParents(File file) {
    List<File> parents = Lists.newArrayList();
    
    synchronized (parent) {
      while ((file = file.getParentFile()) != null) {
        parents.add(file);
      }
      
      for (File parent : Lists.reverse(parents)) {
        Bukkit.getLogger().warning("Creating parent: " + parent.getCanonicalPath());
        parent.mkdir();
      }
    }
  }
}
