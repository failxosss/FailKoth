package net.kothplugin;

import net.kothplugin.commands.KothCommand;
import net.kothplugin.config.ConfigManager;
import net.kothplugin.gui.KothGUI;
import net.kothplugin.hooks.PlaceholderHook;
import net.kothplugin.hooks.VaultHook;
import net.kothplugin.koth.CaptureTaskManager;
import net.kothplugin.koth.HologramManager;
import net.kothplugin.koth.KothManager;
import net.kothplugin.lang.LangManager;
import net.kothplugin.listeners.GuiListener;
import net.kothplugin.listeners.PlayerZoneListener;
import net.kothplugin.scheduler.AutoStartScheduler;
import net.kothplugin.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Vstupní bod pluginu KothPlugin - profesionální King of the Hill systém
 * s podporou 20 jazyků, GUI, statistikami, odměnami a integracemi
 * (Vault, PlaceholderAPI).
 */
public class KothPlugin extends JavaPlugin {

    private static KothPlugin instance;

    private ConfigManager configManager;
    private LangManager langManager;
    private KothManager kothManager;
    private CaptureTaskManager captureTaskManager;
    private HologramManager hologramManager;
    private StatsManager statsManager;
    private AutoStartScheduler autoStartScheduler;
    private VaultHook vaultHook;
    private KothGUI kothGUI;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.langManager = new LangManager(this);
        this.statsManager = new StatsManager(this);
        this.statsManager.init();

        this.captureTaskManager = new CaptureTaskManager(this);
        this.hologramManager = new HologramManager(this);
        this.kothManager = new KothManager(this);
        this.kothManager.load();

        this.kothGUI = new KothGUI(this);

        // příkazy
        KothCommand kothCommand = new KothCommand(this);
        var pluginCommand = getCommand("koth");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(kothCommand);
            pluginCommand.setTabCompleter(kothCommand);
        }

        // listenery
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerZoneListener(this), this);

        // integrace
        this.vaultHook = new VaultHook(this);
        this.vaultHook.setup();

        if (configManager.isPlaceholderApiEnabled() && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI expanze byla zaregistrována.");
        }

        // autostart scheduler
        this.autoStartScheduler = new AutoStartScheduler(this);
        this.autoStartScheduler.start();

        // periodické autosave
        long autosaveTicks = Math.max(1, configManager.getAutosaveIntervalMinutes()) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            statsManager.flush();
            kothManager.saveAll();
        }, autosaveTicks, autosaveTicks);

        getLogger().info("KothPlugin byl úspěšně zapnut! Jazyk: " + configManager.getLanguage()
                + " | Dostupných jazyků: " + langManager.getAvailableLanguages().size());
    }

    @Override
    public void onDisable() {
        if (captureTaskManager != null) {
            captureTaskManager.shutdownAll();
        }
        if (hologramManager != null) {
            hologramManager.removeAll();
        }
        if (autoStartScheduler != null) {
            autoStartScheduler.stop();
        }
        if (kothManager != null) {
            kothManager.saveAll();
        }
        if (statsManager != null) {
            statsManager.shutdown();
        }
        getLogger().info("KothPlugin byl vypnut, data byla uložena.");
    }

    public static KothPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public KothManager getKothManager() {
        return kothManager;
    }

    public CaptureTaskManager getCaptureTaskManager() {
        return captureTaskManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public KothGUI getKothGUI() {
        return kothGUI;
    }
}
