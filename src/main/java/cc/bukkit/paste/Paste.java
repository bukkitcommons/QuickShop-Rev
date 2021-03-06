package cc.bukkit.paste;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.economy.VaultEconProvider;
import org.maxgamer.quickshop.utils.JavaUtils;
import org.maxgamer.quickshop.utils.Util;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.economy.EconomyProvider;
import cc.bukkit.shop.economy.EconomyType;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

/** A util to generate a paste report and upload it to EngineHub/Ubuntu Paste */
@AllArgsConstructor
public class Paste {
  private QuickShop plugin;

  /**
   * Create a server infomation paste
   *
   * @return The paste result content.
   */
  @SneakyThrows
  public @NotNull String genNewPaste() {
    StringBuilder finalReport = new StringBuilder();
    finalReport.append("###############################\n");
    finalReport.append("QuickShop(Rev) Paste Result\n");
    finalReport.append("###############################\n");
    finalReport.append("\n");
    finalReport.append("\n");
    finalReport.append("================================================\n");
    finalReport.append("QuickShop:\n");
    finalReport.append("\tVersion: ").append(Shop.getVersion()).append("\n");
    finalReport.append("\tFork: ").append("Rev").append("\n");
    finalReport.append("\tServer ID: ").append(BaseConfig.serverUUID).append("\n");
    finalReport.append("\tOpenInv Hook: ")
        .append(plugin.getOpenInvPlugin() == null ? "Disabled" : "Enabled").append("\n");
    finalReport.append("\tEconomy System: ");
    try {
      EconomyProvider economyCore = plugin.getEconomy();
      switch (EconomyType.fromID(BaseConfig.economyType)) {
        case VAULT:
          finalReport.append("Vault").append("%")
              .append(((VaultEconProvider) economyCore).getProviderName());
          break;
        case RESERVE:
          finalReport.append("Reserve").append("%").append("No details");
          break;
        case UNKNOWN:
        default:
          finalReport.append("Unknown").append("%").append("Unknown error");
          break;
      }
    } catch (Exception e) {
      finalReport.append("Unknown").append("%").append("Unknown error");
    }

    finalReport.append("\n");
    finalReport.append("================================================\n");
    finalReport.append("System:\n");
    finalReport.append("\tOS: ").append(System.getProperty("os.name")).append("\n");
    finalReport.append("\tArch: ").append(System.getProperty("os.arch")).append("\n");
    finalReport.append("\tVersion: ").append(System.getProperty("os.version")).append("\n");
    finalReport.append("\tCores: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
    finalReport.append("================================================\n");
    finalReport.append("Server:\n");
    finalReport.append("\tName: ").append(Bukkit.getName()).append("\n");
    finalReport.append("\tServer Name: ").append(Bukkit.getServer().getName()).append("\n");
    finalReport.append("\tBuild: ").append(Bukkit.getServer().getVersion()).append("\n");
    finalReport.append("\tNMSV: ").append(Util.getNMSVersion()).append("\n");
    // noinspection deprecation
    finalReport.append("\tData Version: ").append(Bukkit.getUnsafe().getDataVersion()).append("\n");
    finalReport.append("\tJava: ").append(System.getProperty("java.version")).append("\n");
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    List<String> arguments = runtimeMxBean.getInputArguments();
    finalReport.append("\tInput Args: ").append(JavaUtils.list2String(arguments)).append("\n");
    finalReport.append("\tVM Name: ").append(runtimeMxBean.getVmName()).append("\n");
    Map<String, String> sys = runtimeMxBean.getSystemProperties();
    List<String> sysData = new ArrayList<>();
    sys.keySet().forEach(key -> sysData.add(key + "=" + sys.get(key)));
    finalReport.append("\tSystem Properties: ").append(JavaUtils.list2String(sysData)).append("\n");
    finalReport.append("\tPlayers: ").append(Bukkit.getOnlinePlayers().size()).append("/")
        .append(Bukkit.getMaxPlayers()).append("\n");
    finalReport.append("\tOnlineMode: ").append(Bukkit.getOnlineMode()).append("\n");
    finalReport.append("\tBukkitVersion: ").append(Bukkit.getVersion()).append("\n");
    finalReport.append("\tWorldContainer: ").append(Bukkit.getWorldContainer()).append("\n");
    List<String> modules = new ArrayList<>();
    plugin.getIntegrationHelper().getIntegrations().forEach(m -> modules.add(m.getName()));
    finalReport.append("\tLoaded Integrations: ").append(JavaUtils.list2String(modules)).append("\n");
    finalReport.append("================================================\n");
    finalReport.append("Worlds:\n");
    finalReport.append("\tTotal: ").append(Bukkit.getWorlds().size()).append("\n");
    for (World world : Bukkit.getWorlds()) {
      finalReport.append("\t*********************************\n");
      finalReport.append("\t\tName: ").append(world.getName()).append("\n");
      finalReport.append("\t\tEnvironment: ").append(world.getEnvironment().name()).append("\n");
      finalReport.append("\t\tLoaded Chunks: ").append(world.getLoadedChunks().length).append("\n");
      finalReport.append("\t\tPlayer In World: ").append(world.getPlayers().size()).append("\n");
    }
    finalReport.append("\t*********************************\n"); // Add a line after last world
    finalReport.append("================================================\n");
    finalReport.append("Plugins:\n");
    finalReport.append("\tTotal: ").append(Bukkit.getPluginManager().getPlugins().length)
        .append("\n");
    for (Plugin bplugin : Bukkit.getPluginManager().getPlugins()) {
      finalReport.append("\t").append(bplugin.getName()).append("@")
          .append(bplugin.isEnabled() ? "Enabled" : "Disabled").append("\n");
    }
    finalReport.append("================================================\n");
    finalReport.append("Configurations:\n");
    try {
      finalReport.append("\t*********************************\n");
      finalReport.append("\tconfig.yml:\n");
      finalReport.append("\t\t\n")
          .append(new String(
              Objects.requireNonNull(
                  JavaUtils.inputStream2ByteArray(plugin.getDataFolder() + "/config.yml")),
              StandardCharsets.UTF_8))
          .append("\n");
      finalReport.append("\t*********************************\n");
      finalReport.append("\tmessages.json:\n");
      finalReport.append("\t\t\n")
          .append(new String(
              Objects.requireNonNull(
                  JavaUtils.inputStream2ByteArray(plugin.getDataFolder() + "/messages.json")),
              StandardCharsets.UTF_8))
          .append("\n");
      finalReport.append("\t*********************************\n");
      finalReport.append("\t*********************************\n");
      finalReport.append("\titemi18n.yml:\n");
      finalReport.append("\t\t\n")
          .append(new String(
              Objects.requireNonNull(JavaUtils.inputStream2ByteArray(
                  new File(plugin.getDataFolder(), "itemi18n.yml").getPath())),
              StandardCharsets.UTF_8))
          .append("\n");
      finalReport.append("\t*********************************\n");
      finalReport.append("\t*********************************\n");
      finalReport.append("\tenchi18n.yml:\n");
      finalReport.append("\t\t\n")
          .append(new String(
              Objects.requireNonNull(JavaUtils.inputStream2ByteArray(
                  new File(plugin.getDataFolder(), "enchi18n.yml").getPath())),
              StandardCharsets.UTF_8))
          .append("\n");
      finalReport.append("\t*********************************\n");
      finalReport.append("\t*********************************\n");
      finalReport.append("\tpotioni18n.yml:\n");
      finalReport.append("\t\t\n")
          .append(new String(
              Objects.requireNonNull(JavaUtils.inputStream2ByteArray(
                  new File(plugin.getDataFolder(), "potioni18n.yml").getPath())),
              StandardCharsets.UTF_8))
          .append("\n");
      finalReport.append("\t*********************************\n");
      finalReport.append("\t*********************************\n");
      finalReport.append("\tInternal Debug Log:\n");
      finalReport.append("\t\t\n")
          .append(JavaUtils.list2String(Util.getDebugLogs()).replaceAll(",", "\n")).append("\n");
      finalReport.append("\t*********************************\n");
      // try {
      // finalReport.append("\t*********************************\n");
      // finalReport.append("\tlatest.log:\n");
      // finalReport.append("\t\t\n").append(new String(Objects.requireNonNull(Util
      // .inputStream2ByteArray(new File(new File(".", "logs"),
      // "latest.log").getPath())), StandardCharsets.UTF_8)).append("\n");
      // finalReport.append("\t*********************************\n");
      // } catch (Throwable th) {
      // finalReport.append("\t*********************************\n");
      // finalReport.append("\tlatest.log:\n");
      // finalReport.append("\t\t\n").append("Read failed.").append("\n");
      // finalReport.append("\t*********************************\n");
      // }
      try {
        finalReport.append("\t*********************************\n");
        finalReport.append("\tbukkit.yml:\n");
        finalReport.append("\t\t\n")
            .append(new String(
                Objects.requireNonNull(
                    JavaUtils.inputStream2ByteArray(new File(new File("."), "bukkit.yml").getPath())),
                StandardCharsets.UTF_8))
            .append("\n");
        finalReport.append("\t*********************************\n");
      } catch (Throwable th) {
        finalReport.append("\t*********************************\n");
        finalReport.append("\tbukkit.yml:\n");
        finalReport.append("\t\t\n").append("Read failed.").append("\n");
        finalReport.append("\t*********************************\n");
      }
      try {
        finalReport.append("\t*********************************\n");
        finalReport.append("\tspigot.yml:\n");
        finalReport.append("\t\t\n")
            .append(new String(
                Objects.requireNonNull(
                    JavaUtils.inputStream2ByteArray(new File(new File("."), "spigot.yml").getPath())),
                StandardCharsets.UTF_8))
            .append("\n");
        finalReport.append("\t*********************************\n");
      } catch (Throwable th) {
        finalReport.append("\t*********************************\n");
        finalReport.append("\tspigot.yml:\n");
        finalReport.append("\t\t\n").append("Read failed.").append("\n");
        finalReport.append("\t*********************************\n");
      }
      try {
        finalReport.append("\t*********************************\n");
        finalReport.append("\tpaper.yml:\n");
        finalReport.append("\t\t\n")
            .append(new String(
                Objects.requireNonNull(
                    JavaUtils.inputStream2ByteArray(new File(new File("."), "paper.yml").getPath())),
                StandardCharsets.UTF_8))
            .append("\n");
        finalReport.append("\t*********************************\n");
      } catch (Throwable th) {
        finalReport.append("\t*********************************\n");
        finalReport.append("\tpaper.yml:\n");
        finalReport.append("\t\t\n").append("Read failed.").append("\n");
        finalReport.append("\t*********************************\n");
      }
    } catch (Throwable th) {
      finalReport.append("\tFailed to get data\n");
    }
    finalReport.append("================================================\n");

    // Process the data to protect passwords.
    String report = finalReport.toString();
    try {
      ConfigurationSection configurationSection =
          QuickShop.instance().getConfigurationManager().get(BaseConfig.class)
          .conf().getConfigurationSection("database");
      report = report.replaceAll(
          Objects.requireNonNull(configurationSection).getString("user"),
          "[PROTECTED]");
      report = report.replaceAll(configurationSection.getString("password"),
          "[PROTECTED]");
      report = report.replaceAll(configurationSection.getString("host"),
          "[PROTECTED]");
      report = report.replaceAll(configurationSection.getString("port"),
          "[PROTECTED]");
      report = report.replaceAll(configurationSection.getString("database"),
          "[PROTECTED]");
    } catch (Throwable tg) {
      // Ignore
    }
    return report;
  }

  @Nullable
  public String paste(@NotNull String content) {
    PasteInterface paster;
    try {
      // EngineHub Pastebin
      paster = new EngineHubPaster();
      return paster.pasteTheText(content);
    } catch (Exception ignore) {
    }
    try {
      // Ubuntu Pastebin
      paster = new UbuntuPaster();
      return paster.pasteTheText(content);
    } catch (Exception ignore) {
    }
    return null;
  }

  @Nullable
  public String paste(@NotNull String content, int type) {
    PasteInterface paster;
    if (type == 0) {
      try {
        // EngineHub Pastebin
        paster = new EngineHubPaster();
        return paster.pasteTheText(content);
      } catch (Exception ignore) {
      }
    } else {
      try {
        // Ubuntu Pastebin
        paster = new UbuntuPaster();
        return paster.pasteTheText(content);
      } catch (Exception ignore) {
      }
    }
    return null;
  }
}
