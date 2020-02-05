package org.maxgamer.quickshop.utils.messages;

import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.BLACK;
import static org.bukkit.ChatColor.BLUE;
import static org.bukkit.ChatColor.BOLD;
import static org.bukkit.ChatColor.DARK_AQUA;
import static org.bukkit.ChatColor.DARK_BLUE;
import static org.bukkit.ChatColor.DARK_GRAY;
import static org.bukkit.ChatColor.DARK_GREEN;
import static org.bukkit.ChatColor.DARK_PURPLE;
import static org.bukkit.ChatColor.DARK_RED;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.ITALIC;
import static org.bukkit.ChatColor.LIGHT_PURPLE;
import static org.bukkit.ChatColor.MAGIC;
import static org.bukkit.ChatColor.RED;
import static org.bukkit.ChatColor.RESET;
import static org.bukkit.ChatColor.STRIKETHROUGH;
import static org.bukkit.ChatColor.UNDERLINE;
import static org.bukkit.ChatColor.WHITE;
import static org.bukkit.ChatColor.YELLOW;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginLogger;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import com.google.common.collect.Maps;

public class ShopLogger extends PluginLogger {
  /**
   * Regex that indicates the case insensitive
   */
  private final static String PATTERN_IGNORE_CASE = "(?i)";

  /**
   * Mapping from the text pattern of Bukkit color to the corresponding text format of Ansi
   */
  private final static Map<Pattern, String> BUKKIT_TO_ANSI = Maps.newHashMap();

  private final static Map<Level, org.apache.logging.log4j.Level> LOG4J_LEVELS = Maps.newHashMap();
  
  private static boolean hasAnsi = true;
  private static boolean hasJline = true;
  
  static {
    // Colors
    regAnsiMapping(BLACK, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.BLACK);
    regAnsiMapping(DARK_BLUE, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.BLUE);
    regAnsiMapping(DARK_GREEN,
        !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.GREEN);
    regAnsiMapping(DARK_AQUA, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.CYAN);
    regAnsiMapping(DARK_RED, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.RED);
    regAnsiMapping(DARK_PURPLE,
        !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.MAGENTA);
    regAnsiMapping(GOLD, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.YELLOW);
    regAnsiMapping(GRAY, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.WHITE);
    regAnsiMapping(DARK_GRAY, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.BLACK,
        true);
    regAnsiMapping(BLUE, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.BLUE,
        true);
    regAnsiMapping(GREEN, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.GREEN,
        true);
    regAnsiMapping(AQUA, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.CYAN,
        true);
    regAnsiMapping(RED, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.RED, true);
    regAnsiMapping(LIGHT_PURPLE,
        !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.MAGENTA, true);
    regAnsiMapping(YELLOW, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.YELLOW,
        true);
    regAnsiMapping(WHITE, !(hasAnsi && hasJline) ? null : org.fusesource.jansi.Ansi.Color.WHITE,
        true);
    
    // Effects
    regAnsiMapping(MAGIC, org.fusesource.jansi.Ansi.Attribute.BLINK_SLOW);
    regAnsiMapping(BOLD, org.fusesource.jansi.Ansi.Attribute.UNDERLINE_DOUBLE);
    regAnsiMapping(STRIKETHROUGH, org.fusesource.jansi.Ansi.Attribute.STRIKETHROUGH_ON);
    regAnsiMapping(UNDERLINE, org.fusesource.jansi.Ansi.Attribute.UNDERLINE);
    regAnsiMapping(ITALIC, org.fusesource.jansi.Ansi.Attribute.ITALIC);
    regAnsiMapping(RESET, org.fusesource.jansi.Ansi.Attribute.RESET);
    
    LOG4J_LEVELS.put(Level.ALL, org.apache.logging.log4j.Level.ALL);
    LOG4J_LEVELS.put(Level.CONFIG, org.apache.logging.log4j.Level.DEBUG);
    LOG4J_LEVELS.put(Level.FINE, org.apache.logging.log4j.Level.DEBUG);
    LOG4J_LEVELS.put(Level.FINER, org.apache.logging.log4j.Level.TRACE);
    LOG4J_LEVELS.put(Level.FINEST, org.apache.logging.log4j.Level.TRACE);
    LOG4J_LEVELS.put(Level.INFO, org.apache.logging.log4j.Level.INFO);
    LOG4J_LEVELS.put(Level.WARNING, org.apache.logging.log4j.Level.WARN);
    LOG4J_LEVELS.put(Level.SEVERE, org.apache.logging.log4j.Level.ERROR);
    LOG4J_LEVELS.put(Level.OFF, org.apache.logging.log4j.Level.OFF);
  }
  
  private final static ShopLogger SINGLETON_LOGGER = new ShopLogger();
  
  public static ShopLogger instance() {
    return SINGLETON_LOGGER;
  }
  
  @Nullable
  private org.apache.logging.log4j.Logger log4jLogger;
  private boolean useLog4j;

  @SneakyThrows
  private ShopLogger() {
    super(QuickShop.instance());

    // Logger re-naming
    String prefix = QuickShop.instance().getDescription().getPrefix();
    String pluginName = (useLog4j = BaseConfig.useLog4j) ?

        (ChatColor.YELLOW + (prefix == null ? QuickShop.instance().getDescription().getName() : prefix)
            + ChatColor.RESET)
        :

        (prefix != null ? "[" + ChatColor.YELLOW + prefix + ChatColor.RESET + "] "
            : "[" + ChatColor.YELLOW + QuickShop.instance().getDescription().getName() + ChatColor.RESET + "] ");
    pluginName = applyStyles(pluginName);

    // Log4j setup
    if (useLog4j) {
      log4jLogger = LogManager.getLogger(pluginName);
      info("Log4J has been enabled as logging system.");
    } else {
      // Remove logger name from package name
      Field nameField = Logger.class.getDeclaredField("name");
      nameField.setAccessible(true); // private
      nameField.set(this, "");
      
      LOG4J_LEVELS.clear();
    }

    // Apply plugin name for BukkitLogger
    Field pluginNameField = PluginLogger.class.getDeclaredField("pluginName");
    pluginNameField.setAccessible(true); // private
    pluginNameField.set(this, pluginName);

    // Ansi setup
    try {
      hasAnsi = org.fusesource.jansi.Ansi.isEnabled();
    } catch (NoClassDefFoundError e) {
      hasAnsi = false;
      info("Your server do not support Ansi, colour formatter will not be applied.");
    }

    Class<?> main = Class.forName("org.bukkit.craftbukkit.Main"); // Not in subversion
    Field useJline = main.getField("useJline");
    hasJline = useJline.getBoolean(null);
    if (!hasJline) {
      info("As you have turned Jline off, colour formatter will not be applied.");
    }

    this.config();
    // super.setUseParentHandlers(false);
  }

  // Logging stuffs
  @Override
  public void log(LogRecord logRecord) {
    String message = logRecord.getMessage();

    if (message == null) {
      return;
    } else {

      if (useLog4j) {
        log4jLogger.log(
            LOG4J_LEVELS.getOrDefault(logRecord.getLevel(), org.apache.logging.log4j.Level.INFO),
            applyStyles(message));

      } else {
        if (logRecord.getLevel() == Level.WARNING) {
          message = ChatColor.YELLOW + message;
        } else if (logRecord.getLevel() == Level.SEVERE) {
          message = ChatColor.RED + message;
        }

        logRecord.setMessage(applyStyles(message));
        super.log(logRecord);
      }

    }
  }

  /**
   * Collect params as a string with blank spaces between
   *
   * @param params Params
   * @return collected string
   */
  public String collectParams(Object... params) {
    return Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(" "));
  }

  public void info(Object... params) {
    if (useLog4j) {
      log4jLogger.info(collectParams(params));
    } else {
      super.info(collectParams(params));
    }
  }

  public void warning(Object... params) {
    if (useLog4j) {
      log4jLogger.warn(collectParams(params));
    } else {
      super.warning(collectParams(params));
    }
  }

  public void severe(Object... params) {
    if (useLog4j) {
      log4jLogger.error(collectParams(params));
    } else {
      super.severe(collectParams(params));
    }
  }

  public void config(Object... params) {
    if (useLog4j) {
      log4jLogger.debug(collectParams(params));
    } else {
      super.config(collectParams(params));
    }
  }

  public void fine(Object... params) {
    if (useLog4j) {
      log4jLogger.debug(collectParams(params));
    } else {
      super.fine(collectParams(params));
    }
  }

  public void finer(Object... params) {
    if (useLog4j) {
      log4jLogger.trace(collectParams(params));
    } else {
      super.finer(collectParams(params));
    }
  }

  public void finest(Object... params) {
    if (useLog4j) {
      log4jLogger.trace(collectParams(params));
    } else {
      super.finest(collectParams(params));
    }
  }

  // Style stuffs
  /**
   * Apply Ansi styples to the specific message if it contains, internally converting Bukkit style
   * color text to Ansi code or empty string if Ansi is not available.
   *
   * @param message to apply styles
   * @return text maybe applied styles
   */
  public String applyStyles(String message) {
    for (Entry<Pattern, String> entry : BUKKIT_TO_ANSI.entrySet()) {
      message =
          entry.getKey().matcher(message).replaceAll(hasAnsi && hasJline ? entry.getValue() : "");
    }

    return hasAnsi && hasJline ? message.concat(org.fusesource.jansi.Ansi.ansi().reset().toString())
        : message;
  }

  /*
   * Register a mapping from Bukkit color to Ansi
   */
  private static void regAnsiMapping(ChatColor bukkColor, org.fusesource.jansi.Ansi.Color ansiColor) {
    regAnsiMapping0(toPattern(bukkColor), toDesc(ansiColor));
  }

  /*
   * Register a mapping from Bukkit color to Ansi
   */
  private static void regAnsiMapping(ChatColor bukkColor,
      org.fusesource.jansi.Ansi.Attribute ansiAttribute) {
    regAnsiMapping0(toPattern(bukkColor), toDesc(ansiAttribute));
  }

  /*
   * Register a mapping from Bukkit color to Ansi, with color option
   */
  private static void regAnsiMapping(ChatColor bukkColor, org.fusesource.jansi.Ansi.Color ansiColor,
      boolean intensity) {
    regAnsiMapping0(toPattern(bukkColor), toDesc(ansiColor, intensity));
  }

  /*
   * Register a mapping from the pattern of Bukkit color to the description of Ansi, and this is the
   * genuine type for them to be registered.
   */
  private static void regAnsiMapping0(Pattern bukkitPattern, String ansiDesc) {
    BUKKIT_TO_ANSI.put(bukkitPattern, ansiDesc);
  }

  /**
   * Convert a Bukkit color to regex pattern
   *
   * @param bukkitColor the bukkit color
   * @return the pattern
   */
  private static Pattern toPattern(ChatColor bukkitColor) {
    return Pattern.compile(PATTERN_IGNORE_CASE.concat(bukkitColor.toString()));
  }

  /**
   * To populate a Ansi with a reset attribute ahead
   *
   * @param ansiColor
   * @return Ansi with reset ahead
   */
  private static org.fusesource.jansi.Ansi resetWith(org.fusesource.jansi.Ansi.Color ansiColor) {
    return org.fusesource.jansi.Ansi.ansi().a(org.fusesource.jansi.Ansi.Attribute.RESET)
        .fg(ansiColor);
  }

  /**
   * Convert a Ansi to its description text
   *
   * @param ansiColor Ansi
   * @param intensity Ansi color option
   * @return stringified Ansi
   */
  private static String toDesc(org.fusesource.jansi.Ansi.Color ansiColor, boolean intensity) {
    return hasAnsi && hasJline
        ? (intensity ? resetWith(ansiColor).bold().toString() : toDesc(ansiColor))
        : "";
  }

  /**
   * Convert a Ansi to its description text
   *
   * @param ansiColor Ansi
   * @return stringified Ansi
   */
  private static String toDesc(org.fusesource.jansi.Ansi.Color ansiColor) {
    return hasAnsi && hasJline ? resetWith(ansiColor).boldOff().toString() : "";
  }

  /**
   * Convert a Ansi to its description text
   *
   * @param ansiAttribute Ansi
   * @return stringified Ansi
   */
  private static String toDesc(org.fusesource.jansi.Ansi.Attribute ansiAttribute) {
    return hasAnsi && hasJline
        ? org.fusesource.jansi.Ansi.ansi().a(org.fusesource.jansi.Ansi.Attribute.RESET).toString()
        : "";
  }
}
