package org.maxgamer.quickshop.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.utils.github.GithubAPI;
import org.maxgamer.quickshop.utils.github.ReleaseJsonContainer;
import com.google.common.collect.Lists;

public class VersionUpdater {
  private static List<String> getHistoryVersions() throws IOException {
    String hostUrl = "https://www.spigotmc.org/resources/62575/history";
    HttpURLConnection conn = (HttpURLConnection) new URL(hostUrl).openConnection();
    
    conn.setDoInput(true);
    conn.setRequestMethod("GET");
    conn.setRequestProperty("User-Agent", "Chrome/79.0.3945.130");
    
    BufferedReader bufIn = new BufferedReader(
        new InputStreamReader(conn.getInputStream()));
    
    List<String> list = Lists.newArrayList();
    String header = "<td class=\"version\">";
    String tailer = "</td>";
    String line = null;
    
    while ((line = bufIn.readLine()) != null) {
      if (line.startsWith(header) && line.endsWith(tailer))
        list.add(line.substring(header.length(), line.indexOf(tailer)));
    }
    
    return list;
  }
  
  /**
   * Try to obtain a version data for updating.
   * @see VersionData
   * @return the update-ready version data, or empty if not ready.
   */
  public static Optional<VersionData> acquire() {
    if (!BaseConfig.enableUpdater)
      return Optional.empty();

    try {
      List<String> versions = getHistoryVersions();
      int curIndex = versions.indexOf(QuickShop.getVersion());
      
      // Custom build or already latest
      if (curIndex == -1 || curIndex == 0)
        return Optional.empty();
      else
        return Optional.of(VersionData.create(versions.get(0)));
      
    } catch (IOException e) {
      return Optional.empty();
    }
  }
  
  public static byte[] downloadUpdatedJar() throws IOException {
    @Nullable String uurl;
    long uurlSize;
    try {
      ReleaseJsonContainer.AssetsBean bean =
          Objects.requireNonNull(new GithubAPI().getLatestRelease());
      uurl = bean.getBrowser_download_url();
      uurlSize = bean.getSize();
    } catch (Throwable ig) {
      throw new IOException(ig.getMessage());
    }
    
    if (uurl == null) {
      throw new IOException("Failed read the URL, cause it is empty.");
    }
    Util.debug("Downloading from " + uurl);
    InputStream is = HttpRequest.get(new URL(uurl))
        .header("User-Agent", "QuickShop-Reremake " + QuickShop.getVersion()).execute()
        .getInputStream();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    byte[] buff = new byte[1024];
    int len;
    long downloaded = 0;
    if (is == null) {
      throw new IOException("Failed downloading: Cannot open connection with remote server.");
    }
    while ((len = is.read(buff)) != -1) {
      os.write(buff, 0, len);
      downloaded += len;
      Util.debug("File Downloader:  " + downloaded + "/" + uurlSize + " bytes.");
    }
    Util.debug("Downloaded: " + downloaded + " Server:" + uurlSize);
    if (!(uurlSize < 1) && downloaded != uurlSize) {
      Util.debug("Size not match, download may broken.");
      throw new IOException("Size not match, download mayb broken, aborting.");
    }
    Util.debug("Download complete.");
    return os.toByteArray();
  }

  public static void replaceTheJar(byte[] data) throws RuntimeException, IOException {
    File pluginFolder = new File("plugins");
    if (!pluginFolder.exists()) {
      throw new RuntimeException("Can't find the plugins folder.");
    }
    if (!pluginFolder.isDirectory()) {
      throw new RuntimeException("Plugins not a folder.");
    }
    File[] plugins = pluginFolder.listFiles();
    if (plugins == null) {
      throw new IOException("Can't get the files in plugins folder");
    }
    File quickshop = null;
    for (File plugin : plugins) {
      try {
        PluginDescriptionFile desc =
            QuickShop.instance().getPluginLoader().getPluginDescription(plugin);
        if (!desc.getName().equals(QuickShop.instance().getDescription().getName())) {
          continue;
        }
        Util.debug("Selected: " + plugin.getPath());
        quickshop = plugin;
        break;
      } catch (InvalidDescriptionException e) { // Ignore }
      }
    }
    if (quickshop == null) {
      throw new RuntimeException("Failed to get QuickShop Jar File.");
    }
    OutputStream outputStream = new FileOutputStream(quickshop, false);
    outputStream.write(data);
    outputStream.flush();
    outputStream.close();
  }
}
