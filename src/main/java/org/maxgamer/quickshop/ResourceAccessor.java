/*
 * This file is a part of project QuickShop, the name is Language.java Copyright (C) Ghost_chu
 * <https://github.com/Ghost-chu> Copyright (C) Bukkit Commons Studio and contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.maxgamer.quickshop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.files.Rewriter;

public class ResourceAccessor {
  /**
   * Get target language's type file.
   *
   * @param language The target language
   * @param type The file type for you want get. e.g. messages
   * @return The target file's InputStream.
   */
  public InputStream getFile(@Nullable String language, @Nullable String type) {
    if (language == null) {
      language = "en";
      Util.debug("Using the default language (EN) cause language is null.");
    }
    if (type == null || type.isEmpty()) {
      throw new IllegalArgumentException("Type cannot be null or empty");
    }
    InputStream inputStream = QuickShop.instance().getResource(type + "/" + language + ".json");
    if (inputStream == null) {
      Util.debug("Using the default language because we can't get the InputStream.");
      inputStream = QuickShop.instance().getResource(type + "/" + "en" + ".json");
    }
    return inputStream;
    // File name should call type-language.yml ---> config-zh.yml
  }

  // Write file under plugin folder

  /**
   * Save the target language's type file to the datafolder
   *
   * @param language Target language
   * @param type Target type
   * @param fileName The filename you want write to the plugin datafolder.
   */
  public void saveFile(@NotNull String language, @NotNull String type, @NotNull String fileName) {
    File targetFile = new File(QuickShop.instance().getDataFolder(), fileName);
    if (!targetFile.exists()) {
      try {
        targetFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      InputStream is = getFile(language, type);
      new Rewriter(targetFile).accept(is);
      is.close();
    } catch (Exception err) {
      err.printStackTrace();
    }
  }
}