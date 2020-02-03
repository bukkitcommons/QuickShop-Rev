package org.maxgamer.quickshop.utils.mojang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;

public class AssetJson {
  final String pathTemplate = "minecraft/lang/{0}.json";
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
      Util.debugLog("Cannot parse the json: " + this.gameAssets);
      return null;
    }
    JsonElement obje = json.get("objects");
    if (obje == null) {
      Util.debugLog("Json element is null for json " + this.gameAssets);
      return null;
    }
    JsonObject objs = obje.getAsJsonObject();
    if (objs == null || objs.isJsonNull()) {
      Util.debugLog("Json object is null.");
      return null;
    }
    JsonObject langObj = objs.getAsJsonObject(MsgUtil.fillArgs(pathTemplate, languageCode));
    if (langObj == null || langObj.isJsonNull()) {
      Util.debugLog("Cannot find request path.");
      Util.debugLog(this.gameAssets);
      return null;
    }
    JsonPrimitive hashObj = langObj.getAsJsonPrimitive("hash");
    if (hashObj == null || hashObj.isJsonNull()) {
      Util.debugLog("Cannot get hash.");
      return null;
    }
    return hashObj.getAsString();
  }
}
