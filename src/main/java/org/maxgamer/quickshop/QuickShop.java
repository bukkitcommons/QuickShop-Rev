package org.maxgamer.quickshop;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.command.QuickShopCommands;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.configuration.DatabaseConfig;
import org.maxgamer.quickshop.configuration.DisplayConfig;
import org.maxgamer.quickshop.configuration.MatcherConfig;
import org.maxgamer.quickshop.database.DatabaseHelper;
import org.maxgamer.quickshop.integration.FactionsIntegration;
import org.maxgamer.quickshop.integration.PlotSquaredIntegration;
import org.maxgamer.quickshop.integration.ResidenceIntegration;
import org.maxgamer.quickshop.integration.TownyIntegration;
import org.maxgamer.quickshop.integration.WorldGuardIntegration;
import org.maxgamer.quickshop.listeners.BlockListener;
import org.maxgamer.quickshop.listeners.ChatListener;
import org.maxgamer.quickshop.listeners.ClearLaggListener;
import org.maxgamer.quickshop.listeners.CustomInventoryListener;
import org.maxgamer.quickshop.listeners.DisplayBugFixListener;
import org.maxgamer.quickshop.listeners.DisplayProtectionListener;
import org.maxgamer.quickshop.listeners.InternalListener;
import org.maxgamer.quickshop.listeners.LockListener;
import org.maxgamer.quickshop.listeners.ShopActionListener;
import org.maxgamer.quickshop.listeners.ShopProtector;
import org.maxgamer.quickshop.permission.PermissionManager;
import org.maxgamer.quickshop.scheduler.AsyncLogWatcher;
import org.maxgamer.quickshop.scheduler.OngoingFeeWatcher;
import org.maxgamer.quickshop.scheduler.ScheduledSignUpdater;
import org.maxgamer.quickshop.scheduler.SyncDisplayDespawner;
import org.maxgamer.quickshop.scheduler.UpdateWatcher;
import org.maxgamer.quickshop.shop.QuickShopLoader;
import org.maxgamer.quickshop.shop.QuickShopManager;
import org.maxgamer.quickshop.shop.QuickShopActionManager;
import org.maxgamer.quickshop.utils.BuildPerms;
import org.maxgamer.quickshop.utils.FunnyEasterEgg;
import org.maxgamer.quickshop.utils.ItemMatcher;
import org.maxgamer.quickshop.utils.NoCheatPlusExemptor;
import org.maxgamer.quickshop.utils.SentryErrorReporter;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.nms.ReflectionUtil;
import org.maxgamer.quickshop.utils.wrappers.bukkit.BukkitWrapper;
import org.maxgamer.quickshop.utils.wrappers.bukkit.PaperWrapper;
import org.maxgamer.quickshop.utils.wrappers.bukkit.SpigotWrapper;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopActionManager;
import cc.bukkit.shop.ShopLoader;
import cc.bukkit.shop.ShopManager;
import cc.bukkit.shop.ShopPlugin;
import cc.bukkit.shop.configuration.ConfigurationData;
import cc.bukkit.shop.configuration.ConfigurationManager;
import cc.bukkit.shop.database.Database;
import cc.bukkit.shop.database.Dispatcher;
import cc.bukkit.shop.database.connector.DatabaseConnector;
import cc.bukkit.shop.database.connector.MySQLConnector;
import cc.bukkit.shop.database.connector.SQLiteConnector;
import cc.bukkit.shop.economy.EconomyProvider;
import cc.bukkit.shop.economy.EconomyType;
import cc.bukkit.shop.economy.impl.ReserveEconProvider;
import cc.bukkit.shop.economy.impl.VaultEconProvider;
import cc.bukkit.shop.integration.IntegrateStage;
import cc.bukkit.shop.integration.IntegrationHelper;
import lombok.Getter;
import me.minebuilders.clearlag.Clearlag;
import me.minebuilders.clearlag.listeners.ItemMergeListener;

@Getter
public final class QuickShop extends JavaPlugin implements ShopPlugin {
  /**
   * This is only a reference of the internal instance.
   * @see QuickShop#instance()
   */
  @Deprecated
  public static QuickShop instance;
  
  private static QuickShop singleton;
  
  public static QuickShop instance() {
    return singleton;
  }

  private IntegrationHelper integrationHelper;
  // Listeners (These don't)
  private final BlockListener blockListener = new BlockListener();
  /** The BootError, if it not NULL, plugin will stop loading and show setted errors when use /qs */
  @Nullable
  private IssuesHelper bootError;
  // Listeners - We decide which one to use at runtime
  private final ChatListener chatListener = new ChatListener();
  private QuickShopCommands commandManager;
  /** WIP */
  private NoCheatPlusExemptor compatibilityTool = new NoCheatPlusExemptor();

  private final CustomInventoryListener customInventoryListener = new CustomInventoryListener(this);
  
  /** The database for storing all our data for persistence */
  @NotNull
  private Database database;
  /** Contains all SQL tasks */
  @NotNull
  private DatabaseHelper databaseHelper;
  
  /** Queued database manager */
  @NotNull
  private Dispatcher dispatcher;

  @NotNull
  private final DisplayBugFixListener displayBugFixListener = new DisplayBugFixListener(this);
  private int displayItemCheckTicks;
  /** The economy we hook into for transactions */
  @NotNull
  private EconomyProvider economy;

  @NotNull
  private final DisplayProtectionListener inventoryListener = new DisplayProtectionListener();
  @NotNull
  private final ItemMatcher itemMatcher = new ItemMatcher();
  /** Language manager, to select which language will loaded. */
  @NotNull
  private final ResourceAccessor language = new ResourceAccessor();

  /** Whether or not to limit players shop amounts */
  private boolean limit = false;

  /** The shop limites. */
  @NotNull
  private HashMap<String, Integer> limits = new HashMap<>();

  @NotNull
  private final LockListener lockListener = new LockListener(this);
  // private BukkitTask itemWatcherTask;
  @Nullable
  private AsyncLogWatcher logWatcher;
  /** bStats, good helper for metrics. */
  @NotNull
  private final Metrics metrics = new Metrics(this);

  private boolean noopDisable;
  /** The plugin OpenInv (null if not present) */
  private Plugin openInvPlugin;
  /** The plugin PlaceHolderAPI(null if not present) */
  private Plugin placeHolderAPI;
  /** A util to call to check some actions permission */
  private BuildPerms permissionChecker;

  @NotNull
  private final ShopActionListener playerListener = new ShopActionListener();
  @NotNull
  private final InternalListener internalListener = new InternalListener(this);
  /**
   * Whether we players are charged a fee to change the price on their shop (To help deter endless
   * undercutting
   */
  private boolean priceChangeRequiresFee = false;
  /** The error reporter to help devs report errors to Sentry.io */
  @NotNull
  private SentryErrorReporter sentryErrorReporter;

  @NotNull
  private final ShopProtector shopProtectListener = new ShopProtector();
  // private ShopVaildWatcher shopVaildWatcher;
  private SyncDisplayDespawner displayAutoDespawnWatcher;
  /** A set of players who have been warned ("Your shop isn't automatically locked") */


  @NotNull
  private final OngoingFeeWatcher ongoingFeeWatcher = new OngoingFeeWatcher(this);
  private ScheduledSignUpdater signUpdateWatcher;
  private BukkitWrapper bukkitAPIWrapper;
  private boolean enabledAsyncDisplayDespawn;
  
  @Getter
  @NotNull
  private final ConfigurationManager configurationManager = ConfigurationManager.createManager(this);

  /**
   * Returns QS version, this method only exist on QSRR forks If running other QSRR forks,, result
   * may not is "Reremake x.x.x" If running QS offical, Will throw exception.
   *
   * @return Plugin Version
   */
  public String getVersion() {
    return getDescription().getVersion();
  }

  /**
   * Get the Player's Shop limit.
   *
   * @param p The player you want get limit.
   * @return int Player's shop limit
   */
  public int getShopLimit(@NotNull Player p) {
    int max = BaseConfig.defaultLimits;
    for (Entry<String, Integer> entry : limits.entrySet()) {
      if (entry.getValue() > max && PermissionManager.instance().has(p, entry.getKey())) {
        max = entry.getValue();
      }
    }
    return max;
  }

  /** Load 3rdParty plugin support module. */
  private void loadSoftdepends() {
    // added for compatibility reasons with OpenInv - see
    // https://github.com/KaiKikuchi/QuickShop/issues/139
    if (BaseConfig.openInv) {
      this.openInvPlugin = Bukkit.getPluginManager().getPlugin("OpenInv");
      if (this.openInvPlugin != null) {
        getLogger().info("Successfully loaded OpenInv support!");
      }
    }
    if (BaseConfig.placeHolderAPI) {
      this.placeHolderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
      if (this.placeHolderAPI != null) {
        getLogger().info("Successfully loaded PlaceHolderAPI support!");
      }
    }
    if (DisplayConfig.displayItems) {
      if (Bukkit.getPluginManager().getPlugin("ClearLag") != null) {
        try {
          Clearlag clearlag = (Clearlag) Bukkit.getPluginManager().getPlugin("ClearLag");
          for (RegisteredListener clearLagListener : ItemSpawnEvent.getHandlerList()
              .getRegisteredListeners()) {
            if (!clearLagListener.getPlugin().equals(clearlag)) {
              continue;
            }
            int spamTimes = 500;
            if (clearLagListener.getListener().getClass().equals(ItemMergeListener.class)) {
              ItemSpawnEvent.getHandlerList().unregister(clearLagListener.getListener());
              for (int i = 0; i < spamTimes; i++) {
                getLogger().warning("+++++++++++++++++++++++++++++++++++++++++++");
                getLogger().severe(
                    "Detected incompatible module of ClearLag-ItemMerge module, it will broken the QuickShop display, we already unregister this module listener!");
                getLogger().severe(
                    "Please turn off it in the ClearLag config.yml or turn off the QuickShop display feature!");
                getLogger().severe(
                    "If you didn't do that, this message will keep spam in your console every times you server boot up!");
                getLogger().warning("+++++++++++++++++++++++++++++++++++++++++++");
                getLogger().info("This message will spam more " + (spamTimes - i) + " times!");
              }
            }
          }
        } catch (Throwable ignored) {
        }
      }
    }
  }

  /**
   * Tries to load the economy and its core. If this fails, it will try to use vault. If that fails,
   * it will return false.
   *
   * @return true if successful, false if the core is invalid or is not found, and vault cannot be
   *         used.
   */
  private boolean loadEconomy() {
    EconomyProvider core;
    switch (EconomyType.fromID(BaseConfig.economyType)) {
      case VAULT:
        economy = core = new VaultEconProvider();
        break;
      case RESERVE:
        economy = core = new ReserveEconProvider();
        break;
      default:
        IssuesHelper.econError();
        Util.debug("No economy provider can be selected.");
        return false;
    }
    
    QuickShop.instance().log("Economy Provider: " + core.getName());
    
    if (!core.isValid()) {
      IssuesHelper.econError();
      Util.debug("Economy provider is not vaild: " + core.getName());
      return false;
    }
    
    return true;
  }

  /**
   * Logs the given string to qs.log, if QuickShop is configured to do so.
   *
   * @param s The string to log. It will be prefixed with the date and time.
   */
  public void log(@NotNull String s) {
    if (this.getLogWatcher() == null) {
      return;
    }
    this.getLogWatcher().log(s);
  }

  /** Reloads QuickShops config */
  @Override
  public void reloadConfig() {
    super.reloadConfig(); // This cannot be removed, or NPE
    
    configurationManager.load(QuickShop.class);
    configurationManager.load(BaseConfig.class);
    configurationManager.load(DisplayConfig.class);
    configurationManager.load(MatcherConfig.class);
    configurationManager.load(DatabaseConfig.class);
    
    priceChangeRequiresFee = BaseConfig.priceModFee > 0;
    displayItemCheckTicks = BaseConfig.displayItemCheckTicks;
    
    if (BaseConfig.logActions)
      logWatcher = new AsyncLogWatcher(this, new File(getDataFolder(), "qs.log"));
  }

  /** Early than onEnable, make sure instance was loaded in first time. */
  @Override
  public void onLoad() {
    try {
      onLoad0();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
  
  public QuickShop() {
    super();
    singleton = instance = this;
    ShopLogger.initalize(this);
  }

  protected QuickShop(@NotNull final JavaPluginLoader loader, @NotNull final PluginDescriptionFile description, @NotNull final File dataFolder, @NotNull final File file) {
    super(loader, description, dataFolder, file);
    singleton = instance = this;
    ShopLogger.initalize(this);
}
  
  private void onLoad0() throws IllegalArgumentException, IllegalAccessException {
    try {
      Field logger = ReflectionUtil.getField(JavaPlugin.class, "logger");
      if (logger != null) {
        logger.set(this, ShopLogger.instance());
        // Note: Do this after onLoad
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    
    boolean fine = IssuesHelper.scanDupeInstall();
    ChatColor sideColor = fine ? org.bukkit.ChatColor.DARK_GREEN : ChatColor.DARK_RED;
    ChatColor coreColor = fine ? org.bukkit.ChatColor.GREEN : ChatColor.RED;
    
    System.out.println(
        sideColor + "  __  __   ||   \r\n" + 
        coreColor + "" + " /  \\(_    ||   QuickShop - Rev\r\n" + 
        coreColor + "" + " \\_\\/__)   ||   Version  " + getVersion().substring(4) + "\r\n" +
        sideColor + "           ||   ");
    
    getLogger().info("Developers: " + Util.list2String(getDescription().getAuthors()));
    getLogger().info("Original Author: Netherfoam, Timtower, KaiNoMood");
    
    getDataFolder().mkdirs();

    getLogger().info("Loading up integration modules.");
    this.integrationHelper = new IntegrationHelper();
    this.integrationHelper.callIntegrationsLoad(IntegrateStage.LOAD);
    if (getConfig().getBoolean("integration.worldguard.enable")) {
      Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
      Util.debug("Check WG plugin...");
      if (wg != null) {
        Util.debug("Loading WG modules.");
        this.integrationHelper.register(new WorldGuardIntegration()); // WG require register
                                                                          // flags when onLoad
                                                                          // called.
      }
    }

    this.integrationHelper.callIntegrationsLoad(IntegrateStage.POST_LOAD);
  }

  @Override
  public void onDisable() {
    if (noopDisable)
      return;
    
    this.integrationHelper.callIntegrationsLoad(IntegrateStage.UNLOAD);
    getLogger().info("QuickShop is finishing remaining work, this may need a while...");

    Util.debug("Closing all GUIs...");
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.closeInventory();
    }
    Util.debug("Unloading all shops...");
    try {
      Shop.getManager().clear();
    } catch (Throwable th) {
      // ignore, we didn't care that
    }

    Util.debug("Cleaning up database queues...");
    dispatcher.close();

    Util.debug("Unregistering tasks...");
    // if (itemWatcherTask != null)
    // itemWatcherTask.cancel();
    if (logWatcher != null) {
      logWatcher.close(); // Closes the file
    }
    /* Unload UpdateWatcher */
    UpdateWatcher.uninit();
    Util.debug("Cleaning up resources and unloading all shops...");
    /* Remove all display items, and any dupes we can find */
    Shop.getActions().clear();
    Shop.getManager().clear();
    /* Close Database */
    if (database != null) {
      try {
        this.database.getConnection().close();
      } catch (SQLException e) {
        if (getSentryErrorReporter() != null) {
          this.getSentryErrorReporter().ignoreThrow();
        }
        e.printStackTrace();
      }
    }
    
    // this.reloadConfig();
    Util.debug("Calling integrations...");
    this.integrationHelper.callIntegrationsLoad(IntegrateStage.POST_UNLOAD);
    Util.debug("All shutdown work is finished.");
  }

  @Override
  public void onEnable() {
    long start = System.currentTimeMillis();
    sentryErrorReporter = new SentryErrorReporter(this);
    
    /*
     * Configs
     */
    reloadConfig();
    getConfig().options().copyDefaults(true);
    saveDefaultConfig();
    Util.loadFromConfig();
    
    /*
     * Locales
     */
    try {
      MsgUtil.loadCfgMessages();
    } catch (Exception e) {
      getLogger().warning("An error throws when loading messages");
      e.printStackTrace();
    }
    MsgUtil.loadItemi18n();
    MsgUtil.loadEnchi18n();
    MsgUtil.loadPotioni18n();
    
    /*
     * Economy
     */
    Util.debug("Loading economy system...");
    loadEconomy();
    
    /*
     * Third party
     */
    loadSoftdepends();
    registerIntegrations();
    integrationHelper.callIntegrationsLoad(IntegrateStage.ENABLE);
    
    /*
     * Commands
     */
    commandManager = new QuickShopCommands();
    getCommand("qs").setExecutor(commandManager);
    getCommand("qs").setTabCompleter(commandManager);
    
    switch (BaseConfig.serverPlatform) {
      case "Spigot":
        bukkitAPIWrapper = new SpigotWrapper();
        getLogger().info(
            "Plugin now running under Spigot mode. Paper performance profile is disabled, if you switch to Paper, we can use a lot paper api to improve the server performance.");
      case "Paper":
        bukkitAPIWrapper = new PaperWrapper();
        getLogger().info("Plugin now running under Paper mode.");
      case "":
      default:
        if (Util.isClassAvailable("com.destroystokyo.paper.PaperConfig")) {
          bukkitAPIWrapper = new PaperWrapper();
          getLogger().info("Plugin now running under Paper mode.");
        } else {
          bukkitAPIWrapper = new SpigotWrapper();
          getLogger().info(
              "Plugin now running under Spigot mode. Paper performance profile is disabled, if you switch to Paper, we can use a lot paper api to improve the server performance.");
        }
    }

    boolean success = setupDatabase(); // Load the database
    if (!success) {
      noopDisable = true;
      ShopLogger.instance().severe("Fatal error: Failed to setup database");
      Bukkit.getPluginManager().disablePlugin(this, true);
      return;
    }

    /* Initalize the tools */
    // Create the shop manager.
    // This should be inited before shop manager
    if (DisplayConfig.displayItems) {
      if (getConfig().getBoolean("shop.display-auto-despawn")) {
        this.displayAutoDespawnWatcher = new SyncDisplayDespawner();
        Bukkit.getScheduler().runTaskTimer(QuickShop.instance(), this.displayAutoDespawnWatcher, 20,
            getConfig().getInt("shop.display-check-time"));
      }
    }
    this.permissionChecker = new BuildPerms();

    if (BaseConfig.enableLimits) {
      this.limit = BaseConfig.enableLimits;
      for (Map<String, Integer> key : BaseConfig.limitRanks) {
        for (Entry<String, Integer> e : key.entrySet())
          limits.put(e.getKey(), e.getValue());
      }
    }
    if (getConfig().getInt("shop.find-distance") > 100) {
      getLogger()
          .severe("Shop.find-distance is too high! It may cause lag! Pick a number under 100!");
    }

    signUpdateWatcher = new ScheduledSignUpdater();

    /* Load all shops. */
    QuickShopLoader.instance().loadShops();

    Bukkit.getPluginManager().registerEvents(blockListener, this);
    Bukkit.getPluginManager().registerEvents(playerListener, this);
    Bukkit.getPluginManager().registerEvents(chatListener, this);
    Bukkit.getPluginManager().registerEvents(inventoryListener, this);
    Bukkit.getPluginManager().registerEvents(customInventoryListener, this);
    Bukkit.getPluginManager().registerEvents(displayBugFixListener, this);
    Bukkit.getPluginManager().registerEvents(shopProtectListener, this);
    Bukkit.getPluginManager().registerEvents(shopProtectListener, this);
    Bukkit.getPluginManager().registerEvents(internalListener, this);
    
    Util.debug("Registering shop watcher...");
    Bukkit.getScheduler().runTaskTimer(this, signUpdateWatcher, 40, 40);
    if (logWatcher != null) {
      Bukkit.getScheduler().runTaskTimerAsynchronously(this, logWatcher, 0, 10);
      getLogger().info("Log actions is enabled, actions will log in the qs.log file!");
    }
    if (getConfig().getBoolean("shop.ongoing-fee.enable")) {
      getLogger().info("Ongoing fee feature is enabled.");
      ongoingFeeWatcher.runTaskTimerAsynchronously(this, 0,
          getConfig().getInt("shop.ongoing-fee.ticks"));
    }
    
    UpdateWatcher.init();
    submitMeritcs();
    
    if (BaseConfig.lock) {
      Bukkit.getPluginManager().registerEvents(lockListener, this);
    }
    if (Bukkit.getPluginManager().getPlugin("ClearLag") != null) {
      Bukkit.getPluginManager().registerEvents(new ClearLaggListener(), this);
    }
    
    getLogger().info("Loading player messages..");
    MsgUtil.loadTransactionMessages();
    MsgUtil.clean();
    
    getLogger().info("QuickShop Loaded! " + (System.currentTimeMillis() - start) + " ms.");
    
    this.integrationHelper.callIntegrationsLoad(IntegrateStage.POST_ENABLE);
    
    try {
      String[] easterEgg = new FunnyEasterEgg().getRandomEasterEgg();
      if (!(easterEgg == null)) {
        Arrays.stream(easterEgg).forEach(str -> getLogger().info(str));
      }
    } catch (Throwable ignore) {
    }
  }

  private void registerIntegrations() {
    Plugin towny = Bukkit.getPluginManager().getPlugin("Towny");
    if (towny != null && towny.isEnabled()) {
      if (getConfig().getBoolean("integration.towny.enable"))
        integrationHelper.register(new TownyIntegration(this));
    }
    
    Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
    if (worldGuard != null && worldGuard.isEnabled()) {
      if (getConfig().getBoolean("integration.worldguard.enable"))
        integrationHelper.register(new WorldGuardIntegration());
    }
    
    Plugin plotSquared = Bukkit.getPluginManager().getPlugin("PlotSquared");
    if (plotSquared != null && plotSquared.isEnabled()) {
      if (getConfig().getBoolean("integration.plotsquared.enable"))
        integrationHelper.register(new PlotSquaredIntegration());
    }
    
    Plugin residence = Bukkit.getPluginManager().getPlugin("Residence");
    if (residence != null && residence.isEnabled()) {
      if (getConfig().getBoolean("integration.residence.enable"))
        integrationHelper.register(new ResidenceIntegration());
    }
    
    Plugin factions = Bukkit.getPluginManager().getPlugin("Factions");
    if (factions != null && factions.isEnabled()) {
      configurationManager.load(FactionsIntegration.class);
      if (FactionsIntegration.enabled)
        integrationHelper.register(new FactionsIntegration());
    }
  }

  /**
   * Setup the database
   *
   * @return The setup result
   */
  private boolean setupDatabase() {
    try {
      DatabaseConnector connector;
      
      if (DatabaseConfig.enableMySQL) {
        ConfigurationData data = QuickShop.instance().getConfigurationManager().get(DatabaseConfig.class);
        YamlConfiguration src = data.conf();
        
        String user = src.getString("settings.mysql.user");
        if (user == null)
          src.set("settings.mysql.user", user = "root");
        
        String pass = src.getString("settings.mysql.password");
        if (pass == null)
          src.set("settings.mysql.password", pass = "passwd");
        
        connector = new MySQLConnector(DatabaseConfig.host,
            user, pass,
            DatabaseConfig.name, DatabaseConfig.port, DatabaseConfig.enableSSL);
      } else {
        connector = new SQLiteConnector(new File(getDataFolder(), "shops.db"));
      }
      
      ShopLogger.instance().info("Database Connector: " + connector.getClass().getSimpleName());
      database = new Database(connector);
      dispatcher = new Dispatcher(database);
      databaseHelper = new DatabaseHelper(dispatcher, database);
      
    } catch (Throwable t) {
      t.printStackTrace();
      IssuesHelper.databaseError();
      return false;
    }
    
    return true;
  }

  private void submitMeritcs() {
    if (!getConfig().getBoolean("disabled-metrics")) {
      String serverVer = Bukkit.getVersion();
      String bukkitVer = Bukkit.getBukkitVersion();
      String vaultVer;
      Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
      if (vault != null) {
        vaultVer = vault.getDescription().getVersion();
      } else {
        vaultVer = "Vault not found";
      }
      // Use internal Metric class not Maven for solve plugin name issues
      String display_Items;
      if (DisplayConfig.displayItems) { // Maybe mod server use this plugin more?
                                                          // Or have big
        // number items need disabled?
        display_Items = "Enabled";
      } else {
        display_Items = "Disabled";
      }
      String locks;
      if (getConfig().getBoolean("shop.lock")) {
        locks = "Enabled";
      } else {
        locks = "Disabled";
      }
      String sneak_action;
      if (getConfig().getBoolean("shop.sneak-to-create")
          || getConfig().getBoolean("shop.sneak-to-trade")) {
        sneak_action = "Enabled";
      } else {
        sneak_action = "Disabled";
      }
      String shop_find_distance = String.valueOf(BaseConfig.findDistance);
      String economyType = EconomyType.fromID(BaseConfig.economyType).name();
      String useDisplayAutoDespawn =
          String.valueOf(BaseConfig.enableDespawner);
      String useEnhanceDisplayProtect =
          String.valueOf(BaseConfig.enhancedDisplayProtection);
      String useEnhanceShopProtect =
          String.valueOf(BaseConfig.enhancedShopProtection);
      String useOngoingFee = String.valueOf(getConfig().getBoolean("shop.ongoing-fee.enable"));
      String disableDebugLoggger = BaseConfig.debugLogger ? "Enabled" : "Disabled";

      // Version
      metrics.addCustomChart(new Metrics.SimplePie("server_version", () -> serverVer));
      metrics.addCustomChart(new Metrics.SimplePie("bukkit_version", () -> bukkitVer));
      metrics.addCustomChart(new Metrics.SimplePie("vault_version", () -> vaultVer));
      metrics.addCustomChart(new Metrics.SimplePie("use_display_items", () -> display_Items));
      metrics.addCustomChart(new Metrics.SimplePie("use_locks", () -> locks));
      metrics.addCustomChart(new Metrics.SimplePie("use_sneak_action", () -> sneak_action));
      metrics.addCustomChart(new Metrics.SimplePie("shop_find_distance", () -> shop_find_distance));
      metrics.addCustomChart(new Metrics.SimplePie("economy_type", () -> economyType));
      metrics.addCustomChart(
          new Metrics.SimplePie("use_display_auto_despawn", () -> useDisplayAutoDespawn));
      metrics.addCustomChart(
          new Metrics.SimplePie("use_enhance_display_protect", () -> useEnhanceDisplayProtect));
      metrics.addCustomChart(
          new Metrics.SimplePie("use_enhance_shop_protect", () -> useEnhanceShopProtect));
      metrics.addCustomChart(new Metrics.SimplePie("use_ongoing_fee", () -> useOngoingFee));
      metrics.addCustomChart(
          new Metrics.SimplePie("disable_background_debug_logger", () -> disableDebugLoggger));
      // Exp for stats, maybe i need improve this, so i add this.
      metrics.submitData(); // Submit now!
      getLogger().info("Metrics submitted.");
    } else {
      getLogger().info("You have disabled mertics, Skipping...");
    }
  }

  /**
   * Return the QSRR's fork edition name, you can modify this if you want create yourself fork.
   *
   * @return The fork name.
   */
  public static String getFork() {
    return "Rev";
  }

  @Override
  public ShopManager getManager() {
    return QuickShopManager.instance();
  }

  @Override
  public ShopLoader getLoader() {
    return QuickShopLoader.instance();
  }

  @Override
  public ShopActionManager getActions() {
    return QuickShopActionManager.instance();
  }
}
