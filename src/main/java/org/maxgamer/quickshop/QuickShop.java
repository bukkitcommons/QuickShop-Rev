package org.maxgamer.quickshop;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.command.QuickShopCommandManager;
import org.maxgamer.quickshop.configuration.BaseConfig;
import org.maxgamer.quickshop.configuration.DatabaseConfig;
import org.maxgamer.quickshop.configuration.DisplayConfig;
import org.maxgamer.quickshop.configuration.MatcherConfig;
import org.maxgamer.quickshop.database.DatabaseHelper;
import org.maxgamer.quickshop.economy.ReserveEconProvider;
import org.maxgamer.quickshop.economy.VaultEconProvider;
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
import org.maxgamer.quickshop.listeners.DisplayProtector;
import org.maxgamer.quickshop.listeners.LockListener;
import org.maxgamer.quickshop.listeners.ShopActionListener;
import org.maxgamer.quickshop.listeners.ShopProtector;
import org.maxgamer.quickshop.messages.QuickShopMessager;
import org.maxgamer.quickshop.permission.QuickShopPermissionManager;
import org.maxgamer.quickshop.scheduler.AsyncLogWatcher;
import org.maxgamer.quickshop.scheduler.OngoingFeeWatcher;
import org.maxgamer.quickshop.scheduler.ScheduledSignUpdater;
import org.maxgamer.quickshop.scheduler.SyncDisplayDespawner;
import org.maxgamer.quickshop.scheduler.UpdateWatcher;
import org.maxgamer.quickshop.shop.QuickShopActionManager;
import org.maxgamer.quickshop.shop.QuickShopItemMatcher;
import org.maxgamer.quickshop.shop.QuickShopLoader;
import org.maxgamer.quickshop.shop.QuickShopManager;
import org.maxgamer.quickshop.utils.BuildPerms;
import org.maxgamer.quickshop.utils.FunnyEasterEgg;
import org.maxgamer.quickshop.utils.JavaUtils;
import org.maxgamer.quickshop.utils.NoCheatPlusExemptor;
import org.maxgamer.quickshop.utils.SentryErrorReporter;
import org.maxgamer.quickshop.utils.Util;
import com.google.common.collect.Maps;
import cc.bukkit.shop.LocaleManager;
import cc.bukkit.shop.PermissionManager;
import cc.bukkit.shop.Shop;
import cc.bukkit.shop.ShopItemMatcher;
import cc.bukkit.shop.ShopLoader;
import cc.bukkit.shop.ShopManager;
import cc.bukkit.shop.ShopMessager;
import cc.bukkit.shop.ShopPlugin;
import cc.bukkit.shop.action.ShopActionManager;
import cc.bukkit.shop.configuration.ConfigurationData;
import cc.bukkit.shop.configuration.ConfigurationManager;
import cc.bukkit.shop.configuration.YamlComments;
import cc.bukkit.shop.database.Database;
import cc.bukkit.shop.database.Dispatcher;
import cc.bukkit.shop.database.connector.DatabaseConnector;
import cc.bukkit.shop.database.connector.MySQLConnector;
import cc.bukkit.shop.database.connector.SQLiteConnector;
import cc.bukkit.shop.economy.EconomyProvider;
import cc.bukkit.shop.economy.EconomyType;
import cc.bukkit.shop.integration.IntegrateStage;
import cc.bukkit.shop.integration.IntegrationManager;
import cc.bukkit.shop.logger.ShopLogger;
import cc.bukkit.shop.util.Reflections;
import cc.bukkit.wrappers.bukkit.BukkitWrapper;
import cc.bukkit.wrappers.bukkit.PaperWrapper;
import cc.bukkit.wrappers.bukkit.SpigotWrapper;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.Getter;

@Getter
public final class QuickShop extends JavaPlugin implements ShopPlugin {
  private static QuickShop singleton;
  
  public static QuickShop instance() {
    return singleton;
  }
  
  /*
   * ShopPlugin implements
   */
  private LocaleManager localeManager;
  
  @NotNull
  private final ShopMessager messager = new QuickShopMessager();
  
  @NotNull
  private final ShopItemMatcher itemMatcher = new QuickShopItemMatcher();
  

  @Override
  public PermissionManager getPermissions() {
    return QuickShopPermissionManager.instance();
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
  
  @Override
  public String getVersion() {
    return getDescription().getVersion();
  }
  
  /*
   * Exclusive implements
   */
  private IntegrationManager integrationHelper; // FIXME API
  
  private QuickShopCommandManager commandManager; // FIXME API
  
  @NotNull
  private final ConfigurationManager configurationManager = ConfigurationManager.createManager(this); // FIXME API
  
  @NotNull
  private final IssuesHelper issuesHelper = IssuesHelper.create();
  
  @NotNull
  private NoCheatPlusExemptor ncpExemptor = new NoCheatPlusExemptor();
  
  @NotNull
  private final Metrics metrics = new Metrics(this);
  
  private EconomyProvider economy;
  
  private BukkitWrapper bukkitAPIWrapper;
  
  @NotNull
  private final HashMap<String, Integer> shopPermLimits = Maps.newHashMap();
  
  /*
   * Softdepend plugins
   */
  private Optional<Plugin> openInvPlugin = Optional.empty();

  private Optional<Plugin> placeHolderAPI = Optional.empty();
  
  /*
   * Database
   */
  private Dispatcher dispatcher;
  
  private Database database;
  
  private DatabaseHelper databaseHelper;
  
  /*
   * Misc
   */
  private AsyncLogWatcher logWatcher;

  private BuildPerms permissionChecker;

  private SentryErrorReporter sentryErrorReporter;

  /**
   * Get the Player's Shop limit.
   *
   * @param p The player you want get limit.
   * @return int Player's shop limit
   */
  public int getShopLimit(@NotNull Player p) {
    int max = BaseConfig.defaultLimits;
    for (Entry<String, Integer> entry : shopPermLimits.entrySet()) {
      if (entry.getValue() > max && QuickShopPermissionManager.instance().has(p, entry.getKey())) {
        max = entry.getValue();
      }
    }
    return max;
  }
  
  private void loadSoftdepends() {
    if (BaseConfig.openInv) {
      openInvPlugin = Optional.ofNullable(Bukkit.getPluginManager().getPlugin("OpenInv"));
      
      if (openInvPlugin.isPresent())
        getLogger().info("Successfully loaded OpenInv support!");
    }
    
    if (BaseConfig.placeHolderAPI) {
      placeHolderAPI = Optional.ofNullable(Bukkit.getPluginManager().getPlugin("PlaceholderAPI"));
      
      if (placeHolderAPI.isPresent())
        getLogger().info("Successfully loaded PlaceHolderAPI support!");
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
        issuesHelper.econError();
        Util.debug("No economy provider can be selected.");
        return false;
    }
    
    QuickShop.instance().log("Economy Provider: " + core.getName());
    
    if (!core.isValid()) {
      issuesHelper.econError();
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
    
    if (BaseConfig.logActions)
      logWatcher = new AsyncLogWatcher(this, new File(getDataFolder(), "qs.log"));
  }
  
  public QuickShop() {
    super();
    singleton = this;
    ShopLogger.initalize(this, BaseConfig.useLog4j);
  }

  protected QuickShop(@NotNull final JavaPluginLoader loader, @NotNull final PluginDescriptionFile description, @NotNull final File dataFolder, @NotNull final File file) {
    super(loader, description, dataFolder, file);
    singleton = this;
    ShopLogger.initalize(this, BaseConfig.useLog4j);
  }

  @Override
  public void onDisable() {
    if (issuesHelper.hasErrored())
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
    Bukkit.getScheduler().runTask(this, () -> {
      try {
        Shop.setPlugin(this);
        enablePlugin();
      } catch (Throwable t) {
        t.printStackTrace();
      }
    });
  }

  public void enablePlugin() {
    try {
      Field logger = Reflections.getField(JavaPlugin.class, "logger");
      if (logger != null) {
        logger.set(this, ShopLogger.instance());
        // Note: Do this after onLoad
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    
    boolean fine = issuesHelper.scanDupeInstall();
    ChatColor sideColor = fine ? org.bukkit.ChatColor.DARK_GREEN : ChatColor.DARK_RED;
    ChatColor coreColor = fine ? org.bukkit.ChatColor.GREEN : ChatColor.RED;
    
    System.out.println(
        sideColor + "  __  __   ||   \r\n" + 
        coreColor + "" + " /  \\(_    ||   QuickShop - Rev\r\n" + 
        coreColor + "" + " \\_\\/__)   ||   Version  " + getVersion().substring(4) + "\r\n" +
        sideColor + "           ||   ");
    
    getLogger().info("Developers: " + JavaUtils.list2String(getDescription().getAuthors()));
    getLogger().info("Original Author: Netherfoam, Timtower, KaiNoMood");
    
    getDataFolder().mkdirs();

    getLogger().info("Loading up integration modules.");
    this.integrationHelper = new IntegrationManager();
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
    
    long start = System.currentTimeMillis();
    Shop.setPlugin(this);
    sentryErrorReporter = new SentryErrorReporter(this);
    new NBTItem(new ItemStack(Material.AIR)); // Initalize to avoid runtime lag
    
    /*
     * Configs
     */
    reloadConfig();
    getConfig().options().copyDefaults(true);
    Util.loadFromConfig();
    
    /*
     * Locales
     */
    localeManager = new QuickShopLocaleManager();
    try {
      localeManager.load();
    } catch (Exception e) {
      getLogger().warning("An error throws when loading messages");
      e.printStackTrace();
    }
    
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
    commandManager = new QuickShopCommandManager();
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
        if (JavaUtils.isClassAvailable("com.destroystokyo.paper.PaperConfig")) {
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
      issuesHelper.databaseError();
      ShopLogger.instance().severe("Fatal error: Failed to setup database");
      Bukkit.getPluginManager().disablePlugin(this, true);
      return;
    }

    /* Initalize the tools */
    // Create the shop manager.
    // This should be inited before shop manager
    if (DisplayConfig.displayItems) {
      if (getConfig().getBoolean("shop.display-auto-despawn")) {
        Bukkit.getScheduler().runTaskTimer(QuickShop.instance(), new SyncDisplayDespawner(), 20,
            getConfig().getInt("shop.display-check-time"));
      }
    }
    this.permissionChecker = new BuildPerms();

    if (BaseConfig.enableLimits) {
      for (Map<String, Integer> key : BaseConfig.limitRanks) {
        for (Entry<String, Integer> e : key.entrySet())
          shopPermLimits.put(e.getKey(), e.getValue());
      }
    }
    if (getConfig().getInt("shop.find-distance") > 100) {
      getLogger()
      .severe("Shop.find-distance is too high! It may cause lag! Pick a number under 100!");
    }

    /* Load all shops. */
    Shop.getLoader().loadShops();

    Bukkit.getPluginManager().registerEvents(new BlockListener(), this);
    Bukkit.getPluginManager().registerEvents(new ShopActionListener(), this);
    Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
    Bukkit.getPluginManager().registerEvents(new DisplayProtector(), this);
    Bukkit.getPluginManager().registerEvents(new CustomInventoryListener(), this);
    Bukkit.getPluginManager().registerEvents(new DisplayBugFixListener(), this);
    Bukkit.getPluginManager().registerEvents(new ShopProtector(), this);

    if (BaseConfig.lock)
      Bukkit.getPluginManager().registerEvents(new LockListener(), this);
    
    if (Bukkit.getPluginManager().getPlugin("ClearLag") != null)
      Bukkit.getPluginManager().registerEvents(new ClearLaggListener(), this);

    Util.debug("Registering shop watcher...");
    Bukkit.getScheduler().runTaskTimer(this, new ScheduledSignUpdater(), 40, 40);
    if (logWatcher != null) {
      Bukkit.getScheduler().runTaskTimerAsynchronously(this, logWatcher, 0, 10);
      getLogger().info("Log actions is enabled, actions will log in the qs.log file!");
    }
    if (getConfig().getBoolean("shop.ongoing-fee.enable")) {
      getLogger().info("Ongoing fee feature is enabled.");
      new OngoingFeeWatcher().runTaskTimerAsynchronously(this, 0,
          getConfig().getInt("shop.ongoing-fee.ticks"));
    }
    
    UpdateWatcher.init();
    submitMeritcs();
    
    getLogger().info("Loading player messages..");
    messager.loadTransactionMessages();
    messager.clean();
    
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
        
        boolean save = false;
        String user = src.getString("settings.mysql.user");
        if (user == null) {
          save = true;
          src.set("settings.mysql.user", user = "root");
        }
        
        String pass = src.getString("settings.mysql.password");
        if (pass == null) {
          save = true;
          src.set("settings.mysql.password", pass = "passwd");
        }
        
        if (save)
          YamlComments.save(data.file(), src); 
        
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
      issuesHelper.databaseError();
      return false;
    }
    
    return true;
  }

  private void submitMeritcs() {
    if (BaseConfig.enableMetrics) {
      String serverVer = Bukkit.getVersion();
      String bukkitVer = Bukkit.getBukkitVersion();
      String economyType = EconomyType.fromID(BaseConfig.economyType).name();
      String disableDebugLoggger = BaseConfig.developerMode ? "Enabled" : "Disabled";

      // Custom charts
      metrics.addCustomChart(new Metrics.SimplePie("server_version", () -> serverVer));
      metrics.addCustomChart(new Metrics.SimplePie("bukkit_version", () -> bukkitVer));
      metrics.addCustomChart(new Metrics.SimplePie("economy_system", () -> economyType));
      metrics.addCustomChart(new Metrics.SimplePie("developer_mode", () -> disableDebugLoggger));
      
      metrics.addCustomChart(new Metrics.SingleLineChart("shops", () -> QuickShopLoader.instance().getShops()));
      metrics.addCustomChart(new Metrics.SingleLineChart("loaded_shops", () -> QuickShopLoader.instance().getLoadedShops()));
      
      metrics.submitData();
    }
  }
}
