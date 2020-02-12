package org.maxgamer.quickshop.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.maxgamer.quickshop.configuration.BaseConfig;
import com.google.common.collect.Lists;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.util.VersionData;

public class VersionDataFetcher {
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
      int curIndex = versions.indexOf(Shop.getVersion());
      
      // Custom build or already latest
      if (curIndex == -1 || curIndex == 0)
        return Optional.empty();
      else
        return Optional.of(VersionData.create(versions.get(0)));
      
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
