package cc.bukkit.shop.configuration;

import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@RequiredArgsConstructor
public class ConfigurationData {
  private final YamlConfiguration conf;
  private final File file;
}
