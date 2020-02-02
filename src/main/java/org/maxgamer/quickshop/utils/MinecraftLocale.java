package org.maxgamer.quickshop.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.SneakyThrows;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.utils.mojang.AssetJson;
import org.maxgamer.quickshop.utils.mojang.MojangAPI;

public class MinecraftLocale {
  @NotNull
  private JsonObject lang;

  @SneakyThrows
  public MinecraftLocale(@NotNull String languageCode) {
    try {
      File cacheFile = new File(Util.getCacheFolder(), "lang.cache"); // Load cache file
      if (!cacheFile.exists())
        cacheFile.createNewFile();

      YamlConfiguration cache = YamlConfiguration.loadConfiguration(cacheFile);
      String serverVersion = ReflectFactory.getServerVersion();

      String cachedNMSVersion = cache.getString("ver", serverVersion);
      String langHash = cache.getString("hash", "");
      String langCode = cache.getString("lang", "");

      boolean fetchData;
      if ("default".equals(languageCode.toLowerCase(Locale.ROOT))) {
        Locale locale = Locale.getDefault();
        langCode = languageCode = locale.getLanguage() + "_" + locale.getCountry();
        fetchData = langHash.isEmpty();
      } else {
        fetchData =
            !languageCode.equals(langCode) ||
            !serverVersion.equals(cachedNMSVersion) || langHash.isEmpty();
      }

      if (fetchData) {
        MojangAPI mojangAPI = new MojangAPI();
        String assetJson = mojangAPI.getAssetIndexJson(serverVersion);

        if (assetJson != null) {
          AssetJson versionJson = new AssetJson(assetJson);
          langHash = versionJson.getLanguageHash(languageCode.toLowerCase());

          if (langHash != null) {
            String langJson = mojangAPI.downloadTextFileFromMojang(langHash);

            if (langJson != null) {
              new Rewriter(new File(Util.getCacheFolder(), langHash))
              .accept(new ByteArrayInputStream(langJson.getBytes(StandardCharsets.UTF_8)));

              cache.set("ver", serverVersion);
              cache.set("hash", langHash);
              cache.set("lang", languageCode);
              cache.save(cacheFile);
            } else {
              Util.debugLog("Cannot download file.");
              QuickShop.instance().getLogger().warning(
                  "Cannot download require files, some items/blocks/potions/enchs language will use default English name.");
            }
          } else {
            Util.debugLog("Cannot get file hash for language " + languageCode.toLowerCase());
            QuickShop.instance().getLogger().warning(
                "Cannot download require files, some items/blocks/potions/enchs language will use default English name.");
          }
        } else {
          Util.debugLog("Cannot get version json.");
          QuickShop.instance().getLogger().warning(
              "Cannot download require files, some items/blocks/potions/enchs language will use default English name.");
        }
      }

      String json = Util.readToString(new File(Util.getCacheFolder(), langHash));
      if (json != null && !json.isEmpty())
        lang = new JsonParser().parse(json).getAsJsonObject();
    } catch (Throwable t) {
      // FIXME stop plugin
      QuickShop.instance().getSentryErrorReporter().ignoreThrow();
      t.printStackTrace();
    }
  }

  /**
   * Get item and block translations, if not found, it will both call getBlock()
   *
   * @param material The material
   * @return The translations for material
   */
  @NotNull
  public String getItem(@NotNull Material material) {
    JsonElement e = lang.get("item.minecraft." + material.name().toLowerCase());
    return e == null ? getBlock(material) : e.getAsString();
  }

  /**
   * Get block only translations, if not found, it WON'T call getItem()
   *
   * @param material The material
   * @return The translations for material
   */
  @NotNull
  public String getBlock(@NotNull Material material) {
    JsonElement e = lang.get("block.minecraft." + material.name().toLowerCase());
    return e == null ? material.name() : e.getAsString();
  }

  /**
   * Get entity translations.
   *
   * @param entity The entity name
   * @return The translations for entity
   */
  @NotNull
  public String getEntity(@NotNull Entity entity) {
    JsonElement e = lang.get("entity.minecraft." + entity.getType().name().toLowerCase());
    return e == null ? entity.getType().name() : e.getAsString();
  }

  /**
   * Get potion/effect translations.
   *
   * @param effect The potion/effect name
   * @return The translations for effect/potions
   */
  @NotNull
  public String getPotion(@NotNull PotionEffectType effect) {
    JsonElement e = lang.get("effect.minecraft." + effect.getName().toLowerCase());
    return e == null ? effect.getName() : e.getAsString();
  }

  /**
   * Get enchantment translations.
   *
   * @param enchantmentName The enchantment name
   * @return The translations for enchantment
   */
  @NotNull
  public String getEnchantment(@NotNull String enchantmentName) {
    JsonElement e = lang.get("enchantment.minecraft." + enchantmentName.toLowerCase());
    return e == null ? enchantmentName : e.getAsString();
  }

  /**
   * Get custom translations.
   *
   * @param node The target node path
   * @return The translations for you custom node path
   */
  @Nullable
  public String get(@NotNull String node) {
    JsonElement e = lang.get(node);
    return e == null ? node : e.getAsString();
  }
}
