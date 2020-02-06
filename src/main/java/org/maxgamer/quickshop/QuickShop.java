package org.maxgamer.quickshop;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import me.minebuilders.clearlag.Clearlag;
import me.minebuilders.clearlag.listeners.ItemMergeListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.command.CommandManager;
import org.maxgamer.quickshop.configuration.ConfigurationManager;
import org.maxgamer.quickshop.configuration.impl.BaseConfig;
import org.maxgamer.quickshop.database.Database;
import org.maxgamer.quickshop.database.DatabaseHelper;
import org.maxgamer.quickshop.database.Dispatcher;
import org.maxgamer.quickshop.database.Database.ConnectionException;
import org.maxgamer.quickshop.database.connector.DatabaseConnector;
import org.maxgamer.quickshop.database.connector.MySQLConnector;
import org.maxgamer.quickshop.database.connector.SQLiteConnector;
import org.maxgamer.quickshop.economy.Economy;
import org.maxgamer.quickshop.economy.EconomyCore;
import org.maxgamer.quickshop.economy.EconomyType;
import org.maxgamer.quickshop.economy.impl.ReserveEconProvider;
import org.maxgamer.quickshop.economy.impl.VaultEconProvider;
import org.maxgamer.quickshop.integration.IntegrateStage;
import org.maxgamer.quickshop.integration.IntegrationHelper;
import org.maxgamer.quickshop.integration.impl.FactionsIntegration;
import org.maxgamer.quickshop.integration.impl.PlotSquaredIntegration;
import org.maxgamer.quickshop.integration.impl.ResidenceIntegration;
import org.maxgamer.quickshop.integration.impl.TownyIntegration;
import org.maxgamer.quickshop.integration.impl.WorldGuardIntegration;
import org.maxgamer.quickshop.listeners.BlockListener;
import org.maxgamer.quickshop.listeners.ChatListener;
import org.maxgamer.quickshop.listeners.ClearLaggListener;
import org.maxgamer.quickshop.listeners.CustomInventoryListener;
import org.maxgamer.quickshop.listeners.DisplayBugFixListener;
import org.maxgamer.quickshop.listeners.DisplayProtectionListener;
import org.maxgamer.quickshop.listeners.InternalListener;
import org.maxgamer.quickshop.listeners.LockListener;
import org.maxgamer.quickshop.listeners.PlayerListener;
import org.maxgamer.quickshop.listeners.ShopProtector;
import org.maxgamer.quickshop.permission.impl.PermissionManager;
import org.maxgamer.quickshop.scheduler.SyncDisplayDespawner;
import org.maxgamer.quickshop.scheduler.AsyncLogWatcher;
import org.maxgamer.quickshop.scheduler.OngoingFeeWatcher;
import org.maxgamer.quickshop.scheduler.ScheduledSignUpdater;
import org.maxgamer.quickshop.scheduler.UpdateWatcher;
import org.maxgamer.quickshop.shop.ShopActionManager;
import org.maxgamer.quickshop.shop.ShopLoader;
import org.maxgamer.quickshop.shop.ShopManager;
import org.maxgamer.quickshop.shop.api.Shop;
import org.maxgamer.quickshop.utils.FunnyEasterEgg;
import org.maxgamer.quickshop.utils.ItemMatcher;
import org.maxgamer.quickshop.utils.BuildPerms;
import org.maxgamer.quickshop.utils.SentryErrorReporter;
import org.maxgamer.quickshop.utils.NoCheatPlusExemptor;
import org.maxgamer.quickshop.utils.Util;
import org.maxgamer.quickshop.utils.messages.MsgUtil;
import org.maxgamer.quickshop.utils.messages.ShopLogger;
import org.maxgamer.quickshop.utils.nms.ReflectFactory;
import org.maxgamer.quickshop.utils.nms.ReflectionUtil;
import org.maxgamer.quickshop.utils.wrappers.bukkit.BukkitWrapper;
import org.maxgamer.quickshop.utils.wrappers.bukkit.PaperWrapper;
import org.maxgamer.quickshop.utils.wrappers.bukkit.SpigotWrapper;

@Getter
public class QuickShop extends JavaPlugin {
  /** The active instance of QuickShop */
  @Deprecated public static QuickShop instance;
  
  private static QuickShop singleton;
  
  public static QuickShop instance() {
    return singleton;
  }
  
  /** The manager to check permissions. */
  private static PermissionManager permissionManager;

  private IntegrationHelper integrationHelper;
  // Listeners (These don't)
  private BlockListener blockListener;
  /** The BootError, if it not NULL, plugin will stop loading and show setted errors when use /qs */
  @Nullable
  private BootError bootError;
  // Listeners - We decide which one to use at runtime
  private ChatListener chatListener;
  private CommandManager commandManager;
  /** WIP */
  private NoCheatPlusExemptor compatibilityTool = new NoCheatPlusExemptor();

  private CustomInventoryListener customInventoryListener;
  /** The database for storing all our data for persistence */
  private Database database;
  /** Contains all SQL tasks */
  private DatabaseHelper databaseHelper;
  /** Queued database manager */
  private Dispatcher databaseManager;

  private DisplayBugFixListener displayBugFixListener;
  private int displayItemCheckTicks;
  /** The economy we hook into for transactions */
  private Economy economy;

  private DisplayProtectionListener inventoryListener;
  private ItemMatcher itemMatcher;
  /** Language manager, to select which language will loaded. */
  private Language language;

  /** Whether or not to limit players shop amounts */
  private boolean limit = false;

  /** The shop limites. */
  private HashMap<String, Integer> limits = new HashMap<>();

  private LockListener lockListener;
  // private BukkitTask itemWatcherTask;
  @Nullable
  private AsyncLogWatcher logWatcher;
  /** bStats, good helper for metrics. */
  private Metrics metrics;

  private boolean noopDisable;
  /** The plugin OpenInv (null if not present) */
  private Plugin openInvPlugin;
  /** The plugin PlaceHolderAPI(null if not present) */
  private Plugin placeHolderAPI;
  /** A util to call to check some actions permission */
  private BuildPerms permissionChecker;

  private PlayerListener playerListener;
  private InternalListener internalListener;
  /**
   * Whether we players are charged a fee to change the price on their shop (To help deter endless
   * undercutting
   */
  private boolean priceChangeRequiresFee = false;
  /** The error reporter to help devs report errors to Sentry.io */
  private SentryErrorReporter sentryErrorReporter;
  /** The server UniqueID, use to the ErrorReporter */
  private UUID serverUniqueID;

  private boolean setupDBonEnableding = false;

  private ShopProtector shopProtectListener;
  // private ShopVaildWatcher shopVaildWatcher;
  private SyncDisplayDespawner displayAutoDespawnWatcher;
  /** A set of players who have been warned ("Your shop isn't automatically locked") */
  private HashSet<String> warnings = new HashSet<>();

  private OngoingFeeWatcher ongoingFeeWatcher;
  private ScheduledSignUpdater signUpdateWatcher;
  private BukkitWrapper bukkitAPIWrapper;
  private boolean enabledAsyncDisplayDespawn;
  
  @Getter
  private ConfigurationManager configurationManager;

  /**
   * Returns QS version, this method only exist on QSRR forks If running other QSRR forks,, result
   * may not is "Reremake x.x.x" If running QS offical, Will throw exception.
   *
   * @return Plugin Version
   */
  public static String getVersion() {
    return QuickShop.instance().getDescription().getVersion();
  }

  /**
   * Get the permissionManager as static
   *
   * @return the permission Manager.
   */
  public static PermissionManager getPermissionManager() {
    return permissionManager;
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
      if (entry.getValue() > max && getPermissionManager().hasPermission(p, entry.getKey())) {
        max = entry.getValue();
      }
    }
    return max;
  }

  /** Load 3rdParty plugin support module. */
  private void load3rdParty() {
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
    if (BaseConfig.displayItems) {
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
  private boolean loadEcon() {
    try {
      // EconomyCore core = new Economy_Vault();
      EconomyCore core = null;
      switch (EconomyType.fromID(BaseConfig.economyType)) {
        case UNKNOWN:
          bootError =
              new BootError("Can't load the Economy provider, invalid value in config.yml.");
          return false;
        case VAULT:
          core = new VaultEconProvider();
          Util.debugLog("Now using the Vault economy system.");
          break;
        case RESERVE:
          core = new ReserveEconProvider();
          Util.debugLog("Now using the Reserve economy system.");
          break;
        default:
          Util.debugLog("No any economy provider selected.");
          break;
      }
      if (core == null) {
        return false;
      }
      if (!core.isValid()) {
        // getLogger().severe("Economy is not valid!");
        bootError = BuiltInSolution.econError();
        // if(econ.equals("Vault"))
        // getLogger().severe("(Does Vault have an Economy to hook into?!)");
        return false;
      } else {
        this.economy = new Economy(core);
        return true;
      }
    } catch (Exception e) {
      this.getSentryErrorReporter().ignoreThrow();
      e.printStackTrace();
      getLogger().severe("QuickShop could not hook into a economy/Not found Vault or Reserve!");
      getLogger().severe("QuickShop CANNOT start!");
      bootError = BuiltInSolution.econError();
      HandlerList.unregisterAll(this);
      getLogger().severe("Plugin listeners was disabled, please fix the economy issue.");
      return false;
    }
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
    configurationManager.load(QuickShop.class);
    configurationManager.load(BaseConfig.class);
    super.reloadConfig();
    // Load quick variables
    // this.display = this.getConfig().getBoolean("shop.display-items");
    this.priceChangeRequiresFee = BaseConfig.priceModFee > 0;
    this.displayItemCheckTicks = BaseConfig.displayItemCheckTicks;
    if (BaseConfig.logActions) {
      logWatcher = new AsyncLogWatcher(this, new File(getDataFolder(), "qs.log"));
    } else {
      logWatcher = null;
    }
  }

  /** Early than onEnable, make sure instance was loaded in first time. */
  @Override
  public void onLoad() {
    singleton = instance = this;
    getDataFolder().mkdirs();
    replaceLogger();

    this.bootError = null;
    getLogger().info("Loading up integration modules.");
    this.integrationHelper = new IntegrationHelper();
    this.integrationHelper.callIntegrationsLoad(IntegrateStage.LOAD);
    if (getConfig().getBoolean("integration.worldguard.enable")) {
      Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
      Util.debugLogHeavy("Check WG plugin...");
      if (wg != null) {
        Util.debugLogHeavy("Loading WG modules.");
        this.integrationHelper.register(new WorldGuardIntegration()); // WG require register
                                                                          // flags when onLoad
                                                                          // called.
      }
    }

    this.integrationHelper.callIntegrationsLoad(IntegrateStage.POST_LOAD);
  }

  @Override
  public void onDisable() {
    if (noopDisable) {
      return;
    }
    this.integrationHelper.callIntegrationsLoad(IntegrateStage.UNLOAD);
    getLogger().info("QuickShop is finishing remaining work, this may need a while...");

    Util.debugLog("Closing all GUIs...");
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.closeInventory();
    }
    Util.debugLog("Unloading all shops...");
    try {
      ShopManager.instance().clear();
    } catch (Throwable th) {
      // ignore, we didn't care that
    }

    Util.debugLog("Cleaning up database queues...");
    if (this.getDatabaseManager() != null) {
      this.getDatabaseManager().flush();
    }

    Util.debugLog("Unregistering tasks...");
    // if (itemWatcherTask != null)
    // itemWatcherTask.cancel();
    if (logWatcher != null) {
      logWatcher.close(); // Closes the file
    }
    /* Unload UpdateWatcher */
    UpdateWatcher.uninit();
    Util.debugLog("Cleaning up resources and unloading all shops...");
    /* Remove all display items, and any dupes we can find */
    ShopActionManager.instance().getActions().clear();
    ShopManager.instance().clear();
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
    if (warnings != null) {
      warnings.clear();
    }
    // this.reloadConfig();
    Util.debugLog("Calling integrations...");
    this.integrationHelper.callIntegrationsLoad(IntegrateStage.POST_UNLOAD);
    Util.debugLog("All shutdown work is finished.");
  }

  @Override
  public void onEnable() {
    long start = System.currentTimeMillis();
    configurationManager = ConfigurationManager.createManager(this);
    configurationManager.load(QuickShop.class);
    this.integrationHelper.callIntegrationsLoad(IntegrateStage.ENABLE);
    /* PreInit for BootError feature */
    commandManager = new CommandManager();
    // noinspection ConstantConditions
    getCommand("qs").setExecutor(commandManager);
    // noinspection ConstantConditions
    getCommand("qs").setTabCompleter(commandManager);

    getLogger().info("Quickshop " + getFork());
    getLogger().info("Reading the configuration...");
    /* Process the config */
    saveDefaultConfig();
    reloadConfig();
    getConfig().options().copyDefaults(true); // Load defaults.
    saveDefaultConfig();
    reloadConfig();
    // getConfig().options().copyDefaults(true);
    if (getConfig().getInt("config-version") == 0) {
      getConfig().set("config-version", 1);
    }
    updateConfig(getConfig().getInt("config-version"));

    getLogger().info("Developers: " + Util.list2String(this.getDescription().getAuthors()));
    getLogger().info("Original author: Netherfoam, Timtower, KaiNoMood");
    getLogger().info("Let's start loading the plugin");

    /* It will generate a new UUID above updateConfig */
    /* Process Metrics and Sentry error reporter. */
    metrics = new Metrics(this);
    // noinspection ConstantConditions
    serverUniqueID =
        UUID.fromString(getConfig().getString("server-uuid", String.valueOf(UUID.randomUUID())));
    sentryErrorReporter = new SentryErrorReporter(this);
    // loadEcon();
    switch (BaseConfig.serverPlatform) {
      case 1:
        bukkitAPIWrapper = new SpigotWrapper();
        getLogger().info(
            "Plugin now running under Spigot mode. Paper performance profile is disabled, if you switch to Paper, we can use a lot paper api to improve the server performance.");
      case 2:
        bukkitAPIWrapper = new PaperWrapper();
        getLogger().info("Plugin now running under Paper mode.");
      default: // AUTO
        if (Util.isClassAvailable("com.destroystokyo.paper.PaperConfig")) {
          bukkitAPIWrapper = new PaperWrapper();
          getLogger().info("Plugin now running under Paper mode.");
        } else {
          bukkitAPIWrapper = new SpigotWrapper();
          getLogger().info(
              "Plugin now running under Spigot mode. Paper performance profile is disabled, if you switch to Paper, we can use a lot paper api to improve the server performance.");
        }
    }

    /* Initalize the Utils */
    itemMatcher = new ItemMatcher();
    Util.initialize();
    try {
      MsgUtil.loadCfgMessages();
    } catch (Exception e) {
      getLogger().warning("An error throws when loading messages");
      e.printStackTrace();
    }
    MsgUtil.loadItemi18n();
    MsgUtil.loadEnchi18n();
    MsgUtil.loadPotioni18n();

    /* Check the running envs is support or not. */
    try {
      runtimeCheck(this);
    } catch (RuntimeException e) {
      bootError = new BootError(e.getMessage());
      return;
    }

    /* Load 3rd party supports */
    load3rdParty();

    setupDBonEnableding = true;
    setupDatabase(); // Load the database
    setupDBonEnableding = false;

    /* Initalize the tools */
    // Create the shop manager.
    permissionManager = new PermissionManager();
    // This should be inited before shop manager
    if (BaseConfig.displayItems) {
      if (getConfig().getBoolean("shop.display-auto-despawn")) {
        this.displayAutoDespawnWatcher = new SyncDisplayDespawner();
        Bukkit.getScheduler().runTaskTimer(QuickShop.instance(), this.displayAutoDespawnWatcher, 20,
            getConfig().getInt("shop.display-check-time"));
      }
    }
    this.databaseManager = new Dispatcher(database);
    this.permissionChecker = new BuildPerms();

    ConfigurationSection limitCfg = this.getConfig().getConfigurationSection("limits");
    if (limitCfg != null) {
      this.limit = limitCfg.getBoolean("use", false);
      limitCfg = limitCfg.getConfigurationSection("ranks");
      for (String key : Objects.requireNonNull(limitCfg).getKeys(true)) {
        limits.put(key, limitCfg.getInt(key));
      }
    }
    if (getConfig().getInt("shop.find-distance") > 100) {
      getLogger()
          .severe("Shop.find-distance is too high! It may cause lag! Pick a number under 100!");
    }

    signUpdateWatcher = new ScheduledSignUpdater();

    /* Load all shops. */
    //ShopLoader.loadShops();

    getLogger().info("Registering Listeners...");
    // Register events

    blockListener = new BlockListener();
    playerListener = new PlayerListener(this);
    chatListener = new ChatListener();
    inventoryListener = new DisplayProtectionListener();
    customInventoryListener = new CustomInventoryListener(this);
    displayBugFixListener = new DisplayBugFixListener(this);
    shopProtectListener = new ShopProtector();
    // shopVaildWatcher = new ShopVaildWatcher(this);
    ongoingFeeWatcher = new OngoingFeeWatcher(this);
    lockListener = new LockListener(this);
    internalListener = new InternalListener(this);

    Bukkit.getPluginManager().registerEvents(blockListener, this);
    Bukkit.getPluginManager().registerEvents(playerListener, this);
    Bukkit.getPluginManager().registerEvents(chatListener, this);
    Bukkit.getPluginManager().registerEvents(inventoryListener, this);
    Bukkit.getPluginManager().registerEvents(customInventoryListener, this);
    Bukkit.getPluginManager().registerEvents(displayBugFixListener, this);
    Bukkit.getPluginManager().registerEvents(shopProtectListener, this);
    Bukkit.getPluginManager().registerEvents(shopProtectListener, this);
    Bukkit.getPluginManager().registerEvents(internalListener, this);
    if (getConfig().getBoolean("shop.lock")) {
      Bukkit.getPluginManager().registerEvents(lockListener, this);
    }
    if (Bukkit.getPluginManager().getPlugin("ClearLag") != null) {
      Bukkit.getPluginManager().registerEvents(new ClearLaggListener(), this);
    }
    getLogger().info("Cleaning MsgUtils...");
    MsgUtil.loadTransactionMessages();
    MsgUtil.clean();
    getLogger().info("Registering UpdateWatcher...");
    UpdateWatcher.init();
    getLogger().info("Registering BStats Mertics...");
    submitMeritcs();
    getLogger().info("QuickShop Loaded! " + (System.currentTimeMillis() - start) + " ms.");
    /* Delay the Ecoonomy system load, give a chance to let economy system regiser. */
    /* And we have a listener to listen the ServiceRegisterEvent :) */
    Util.debugLog("Loading economy system...");
    new BukkitRunnable() {
      @Override
      public void run() {
        loadEcon();
      }
    }.runTaskLater(this, 1);
    Util.debugLog("Registering shop watcher...");
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
    registerIntegrations();
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
    if (getConfig().getBoolean("integration.towny.enable")) {
      Plugin towny = Bukkit.getPluginManager().getPlugin("Towny");
      if (towny != null && towny.isEnabled()) {
        this.integrationHelper.register(new TownyIntegration(this));
      }
    }
    if (getConfig().getBoolean("integration.worldguard.enable")) {
      Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
      if (worldGuard != null && worldGuard.isEnabled()) {
        this.integrationHelper.register(new WorldGuardIntegration());
      }
    }
    if (getConfig().getBoolean("integration.plotsquared.enable")) {
      Plugin plotSquared = Bukkit.getPluginManager().getPlugin("PlotSquared");
      if (plotSquared != null && plotSquared.isEnabled()) {
        this.integrationHelper.register(new PlotSquaredIntegration());
      }
    }
    if (getConfig().getBoolean("integration.residence.enable")) {
      Plugin residence = Bukkit.getPluginManager().getPlugin("Residence");
      if (residence != null && residence.isEnabled()) {
        this.integrationHelper.register(new ResidenceIntegration());
      }
    }
    
    configurationManager.load(FactionsIntegration.class);
    if (FactionsIntegration.enabled) {
      Plugin factions = Bukkit.getPluginManager().getPlugin("Factions");
      if (factions != null && factions.isEnabled()) {
        this.integrationHelper.register(new FactionsIntegration());
      }
    } else {
      ConfigurationManager.createManager(this).unload(FactionsIntegration.class);
    }
  }

  /**
   * Check the env plugin running.
   *
   * @throws RuntimeException The error message, use this to create a BootError.
   */
  private void runtimeCheck(QuickShop shop) throws RuntimeException {
    if (Util.isClassAvailable("org.maxgamer.quickshop.Util.NMS")) {
      getLogger().severe(
          "FATAL: Old QuickShop is installed, You must remove old quickshop jar from plugins folder!");
      throw new RuntimeException(
          "FATAL: Old QuickShop is installed, You must remove old quickshop jar from plugins folder!");
    }
    try {
      getServer().spigot();
    } catch (Throwable e) {
      getLogger().severe("FATAL: QSRR can only be run on Spigot servers and forks of Spigot!");
      throw new RuntimeException("Server must be Spigot based, Don't use CraftBukkit!");
    }

    if (getServer().getName().toLowerCase().contains("catserver")
        || Util.isClassAvailable("moe.luohuayu.CatServer")
        || Util.isClassAvailable("catserver.server.CatServer")) {
      // Send FATAL ERROR TO CatServer's users.
      getLogger().severe("FATAL: QSRR can't run on CatServer Community/Personal/Pro/Async");
      throw new RuntimeException("QuickShop doen't support CatServer");
    }

    if (Util.isDevEdition()) {
      getLogger().severe("WARNING: You are running QSRR in dev-mode");
      getLogger().severe("WARNING: Keep backup and DO NOT run this in a production environment!");
      getLogger().severe("WARNING: Test version may destroy everything!");
      getLogger().severe(
          "WARNING: QSRR won't start without your confirmation, nothing will change before you turn on dev allowed.");
      if (!BaseConfig.developerMode) {
        getLogger().severe(
            "WARNING: Set dev-mode: true in config.yml to allow qs load in dev mode(You may need add this line to the config yourself).");
        noopDisable = true;
        throw new RuntimeException("Snapshot cannot run when dev-mode is false in the config");
      }
    }
    String nmsVersion = Util.getNMSVersion();
    getLogger().info("Running QuickShop-Reremake on NMS version " + nmsVersion
        + " For Minecraft version " + ReflectFactory.getServerVersion());
  }

  /**
   * Setup the database
   *
   * @return The setup result
   */
  private boolean setupDatabase() {
    try {
      ConfigurationSection dbCfg = getConfig().getConfigurationSection("database");
      DatabaseConnector dbCore;
      if (Objects.requireNonNull(dbCfg).getBoolean("mysql")) {
        // MySQL database - Required database be created first.
        // dbPrefix = dbCfg.getString("prefix");
        // if (dbPrefix == null || "none".equals(dbPrefix)) {
        // dbPrefix = "";
        // }
        String user = dbCfg.getString("user");
        String pass = dbCfg.getString("password");
        String host = dbCfg.getString("host");
        String port = dbCfg.getString("port");
        String database = dbCfg.getString("database");
        boolean useSSL = dbCfg.getBoolean("usessl");
        dbCore = new MySQLConnector(Objects.requireNonNull(host, "MySQL host can't be null"),
            Objects.requireNonNull(user, "MySQL username can't be null"),
            Objects.requireNonNull(pass, "MySQL password can't be null"),
            Objects.requireNonNull(database, "MySQL database name can't be null"),
            Objects.requireNonNull(port, "MySQL port can't be null"), useSSL);
      } else {
        // SQLite database - Doing this handles file creation
        dbCore = new SQLiteConnector(new File(this.getDataFolder(), "shops.db"));
      }
      this.database = new Database(dbCore);
      // Make the database up to date
      databaseHelper = new DatabaseHelper(this, database);
    } catch (ConnectionException e) {
      e.printStackTrace();
      if (setupDBonEnableding) {
        bootError = BuiltInSolution.databaseError();
        return false;
      } else {
        getLogger().severe("Error connecting to the database.");
      }
      return false;
    } catch (SQLException e) {
      e.printStackTrace();
      getServer().getPluginManager().disablePlugin(this);
      if (setupDBonEnableding) {
        bootError = BuiltInSolution.databaseError();
        return false;
      } else {
        getLogger().severe("Error setting up the database.");
      }
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
      if (BaseConfig.displayItems) { // Maybe mod server use this plugin more?
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
      String economyType = Economy.getNowUsing().name();
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

  private void updateConfig(int selectedVersion) {
    String serverUUID = getConfig().getString("server-uuid");
    if (serverUUID == null || serverUUID.isEmpty()) {
      serverUUID = BaseConfig.uuid;
      configurationManager.save(BaseConfig.class);
      getConfig().set("server-uuid", serverUUID);
    }
    saveConfig();
    reloadConfig();
    File file = new File(getDataFolder(), "example.config.yml");
    file.delete();
    try {
      Files.copy(Objects.requireNonNull(getResource("config.yml")), file.toPath());
    } catch (IOException ioe) {
      getLogger().warning("Error on spawning the example config file: " + ioe.getMessage());
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

  private void replaceLogger() {
    try {
      Field logger = ReflectionUtil.getField(JavaPlugin.class, "logger");
      if (logger != null) {
        logger.set(this, ShopLogger.instance());
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
