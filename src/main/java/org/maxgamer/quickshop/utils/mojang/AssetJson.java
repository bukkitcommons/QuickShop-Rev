package org.maxgamer.quickshop.utils.mojang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.utils.Util;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class AssetJson {
  final String pathTemplate = "minecraft/lang/{0}.json";
  final String pathTemplateLegacy = "minecraft/lang/{0}.lang";
  @NotNull
  String gameAssets;

  public AssetJson(@NotNull String json) {
    this.gameAssets = json;
  }

  @Nullable
  public String getLanguageHash(@NotNull String languageCode) {
    languageCode = languageCode.replace("-", "_").toLowerCase().trim();
    JsonObject json = new JsonParser().parse(this.gameAssets).getAsJsonObject();
    if (json == null || json.isJsonNull()) {
      Util.debug("Cannot parse the json: " + this.gameAssets);
      return null;
    }
    JsonElement obje = json.get("objects");
    if (obje == null) {
      Util.debug("Json element is null for json " + this.gameAssets);
      return null;
    }
    JsonObject objs = obje.getAsJsonObject();
    if (objs == null || objs.isJsonNull()) {
      Util.debug("Json object is null.");
      return null;
    }
    JsonObject langObj = objs.getAsJsonObject(Util.fillArgs(pathTemplate, languageCode));
    if (langObj == null || langObj.isJsonNull()) {
      langObj = objs.getAsJsonObject(Util.fillArgs(pathTemplateLegacy, languageCode));
      
      if (langObj == null || langObj.isJsonNull()) {
        Util.debug("Cannot find request path.");
        Util.debug(this.gameAssets);
        return null;
      }
    }
    JsonPrimitive hashObj = langObj.getAsJsonPrimitive("hash");
    if (hashObj == null || hashObj.isJsonNull()) {
      Util.debug("Cannot get hash.");
      return null;
    }
    return hashObj.getAsString();
  }
}
