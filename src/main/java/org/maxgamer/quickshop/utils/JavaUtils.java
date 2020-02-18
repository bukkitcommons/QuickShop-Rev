package org.maxgamer.quickshop.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.BaseConfig;

public class JavaUtils {
  public static String fillArgs(@Nullable String raw, @Nullable String... args) {
    if (raw == null) {
      return "Invalid message: null";
    }
    if (raw.isEmpty()) {
      return "";
    }
    if (args == null) {
      return raw;
    }
    for (int i = 0; i < args.length; i++) {
      raw = StringUtils.replace(raw, "{" + i + "}", args[i] == null ? "" : args[i]);
    }
    return raw;
  }

  /**
   * Convert strArray to String. E.g "Foo, Bar"
   *
   * @param strArray Target array
   * @return str
   */
  public static String array2String(@NotNull String[] strArray) {
    switch (strArray.length) {
      case 0:
        return "";
      case 1:
        return strArray[0];
      case 2:
        return strArray[0].concat(", ").concat(strArray[1]);
      default:
        String con = strArray[0].concat(", ");
        for (int i = 0; i < strArray.length - 2; i++) {
          con = con.concat(strArray[i]).concat(", ");
        }
        return con.concat(strArray[strArray.length - 1]);
    }
  }

  /**
   * Formats the given number according to how vault would like it. E.g. $50 or 5 dollars.
   *
   * @param n price
   * @return The formatted string.
   */
  public static String format(double n) {
    if (BaseConfig.disableVaultFormat) {
      return BaseConfig.currencySymbol + n;
    }
    try {
      String formated = QuickShop.instance().getEconomy().format(n);
      if (formated == null || formated.isEmpty()) {
        Util.debug(
            "Use alternate-currency-symbol to formatting, Cause economy plugin returned null");
        return BaseConfig.currencySymbol + n;
      } else {
        return formated;
      }
    } catch (NumberFormatException e) {
      Util.debug("format", e.getMessage());
      Util.debug("format",
          "Use alternate-currency-symbol to formatting, Cause NumberFormatException");
      return BaseConfig.currencySymbol + n;
    }
  }

  /**
   * Read the InputStream to the byte array.
   *
   * @param filePath Target file
   * @return Byte array
   */
  @Nullable
  public static byte[] inputStream2ByteArray(@NotNull String filePath) {
    try {
      InputStream in = new FileInputStream(filePath);
      byte[] data = toByteArray(in);
      in.close();
      return data;
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Get this class available or not
   *
   * @param qualifiedName class qualifiedName
   * @return boolean Available
   */
  public static boolean isClassAvailable(@NotNull String qualifiedName) {
    try {
      Class.forName(qualifiedName);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Check a string is or not a UUID string
   *
   * @param string Target string
   * @return is UUID
   */
  public static boolean isUUID(@NotNull String string) {
    if (string.length() != 36 && string.length() != 32) {
      return false;
    }
    try {
      // noinspection ResultOfMethodCallIgnored
      UUID.fromString(string);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Convert strList to String. E.g "Foo, Bar"
   *
   * @param strList Target list
   * @return str
   */
  public static String list2String(@NotNull List<String> strList) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < strList.size(); i++) {
      builder.append(strList.get(i));
      if (i + 1 != strList.size()) {
        builder.append(", ");
      }
    }
    return builder.toString();
  }

  /**
   * Read the file to the String
   *
   * @param file Target file.
   * @return Target file's content.
   */
  public static String readToString(@NotNull File file) {
    long filelength = file.length();
    byte[] filecontent = new byte[(int) filelength];
    try {
      FileInputStream in = new FileInputStream(file);
      in.read(filecontent);
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new String(filecontent, StandardCharsets.UTF_8);
  }
  
  private static byte[] toByteArray(@NotNull InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024 * 4];
    int n;
    while ((n = in.read(buffer)) != -1) {
      out.write(buffer, 0, n);
    }
    return out.toByteArray();
  }
}
