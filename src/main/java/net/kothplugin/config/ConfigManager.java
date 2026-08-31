package net.kothplugin.config;

import net.kothplugin.KothPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Zapouzdřuje přístup k hlavnímu config.yml a poskytuje typované gettery
 * pro všechny nastavení pluginu.
 */
public class ConfigManager {

    private final KothPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(KothPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration raw() {
        return config;
    }

    public String getLanguage() {
        return config.getString("language", "en");
    }

    public boolean isPerPlayerLanguage() {
        return config.getBoolean("per-player-language.enabled", false);
    }

    public String getStorageType() {
        return config.getString("storage.type", "YAML");
    }

    public String getMysqlHost() { return config.getString("storage.mysql.host", "localhost"); }
    public int getMysqlPort() { return config.getInt("storage.mysql.port", 3306); }
    public String getMysqlDatabase() { return config.getString("storage.mysql.database", "kothplugin"); }
    public String getMysqlUsername() { return config.getString("storage.mysql.username", "root"); }
    public String getMysqlPassword() { return config.getString("storage.mysql.password", ""); }
    public boolean getMysqlUseSSL() { return config.getBoolean("storage.mysql.useSSL", false); }
    public String getMysqlTablePrefix() { return config.getString("storage.mysql.table-prefix", "koth_"); }

    public int getCheckIntervalTicks() {
        return config.getInt("general.check-interval-ticks", 20);
    }

    public int getAutosaveIntervalMinutes() {
        return config.getInt("general.autosave-interval-minutes", 5);
    }

    public boolean isDebug() {
        return config.getBoolean("general.debug", false);
    }

    public int getDefaultCaptureTime() {
        return config.getInt("capture.default-capture-time", 60);
    }

    public double getMultiCaptureMultiplier() {
        return config.getDouble("capture.multi-capture-multiplier", 1.0);
    }

    public int getMaxCaptureBoostPlayers() {
        return config.getInt("capture.max-capture-boost-players", 3);
    }

    public boolean isContestEnabled() {
        return config.getBoolean("capture.contest-enabled", true);
    }

    public String getContestBehaviour() {
        return config.getString("capture.contest-behaviour", "freeze");
    }

    public boolean isDecayEnabled() {
        return config.getBoolean("capture.decay-enabled", true);
    }

    public double getDecayRatePerSecond() {
        return config.getDouble("capture.decay-rate-per-second", 1.0);
    }

    public boolean isWarmupEnabled() {
        return config.getBoolean("warmup.enabled", true);
    }

    public int getDefaultWarmupSeconds() {
        return config.getInt("warmup.default-seconds", 30);
    }

    public boolean isAutostartEnabled() {
        return config.getBoolean("autostart.enabled", false);
    }

    public int getAutostartMinInterval() {
        return config.getInt("autostart.min-interval-minutes", 60);
    }

    public int getAutostartMaxInterval() {
        return config.getInt("autostart.max-interval-minutes", 180);
    }

    public boolean isAutostartRandom() {
        return config.getBoolean("autostart.random-selection", true);
    }

    public boolean isBossbarEnabled() {
        return config.getBoolean("notifications.bossbar.enabled", true);
    }

    public String getBossbarColor() {
        return config.getString("notifications.bossbar.color", "YELLOW");
    }

    public String getBossbarStyle() {
        return config.getString("notifications.bossbar.style", "SOLID");
    }

    public boolean isActionbarEnabled() {
        return config.getBoolean("notifications.actionbar.enabled", true);
    }

    public boolean isTitleEnabled() {
        return config.getBoolean("notifications.title.enabled", true);
    }

    public boolean isScoreboardEnabled() {
        return config.getBoolean("notifications.scoreboard.enabled", true);
    }

    public boolean isChatBroadcastEnabled() {
        return config.getBoolean("notifications.chat-broadcast.enabled", true);
    }

    public boolean isChatBroadcastGlobal() {
        return config.getBoolean("notifications.chat-broadcast.global", true);
    }

    public boolean isSoundEnabled() {
        return config.getBoolean("notifications.sound.enabled", true);
    }

    public String getSound(String key, String def) {
        return config.getString("notifications.sound." + key, def);
    }

    public boolean isGuiEnabled() {
        return config.getBoolean("gui.enabled", true);
    }

    public String getGuiTitle() {
        return config.getString("gui.title", "&8&lKOTH Menu");
    }

    public int getGuiSize() {
        return config.getInt("gui.size", 27);
    }

    public boolean isGuiSoundsEnabled() {
        return config.getBoolean("gui.sounds-enabled", true);
    }

    public boolean isBroadcastWinner() {
        return config.getBoolean("rewards.broadcast-winner", true);
    }

    public boolean isGiveToWholeTeam() {
        return config.getBoolean("rewards.give-to-whole-team", true);
    }

    public boolean isVaultEnabled() {
        return config.getBoolean("integrations.vault.enabled", true);
    }

    public boolean isPlaceholderApiEnabled() {
        return config.getBoolean("integrations.placeholderapi.enabled", true);
    }

    public boolean isWorldGuardEnabled() {
        return config.getBoolean("integrations.worldguard.enabled", false);
    }

    public boolean isHologramsEnabled() {
        return config.getBoolean("integrations.holograms.enabled", false);
    }

    public String getTeamMode() {
        return config.getString("teams.mode", "SOLO");
    }

    public boolean isRequireTeam() {
        return config.getBoolean("teams.require-team", true);
    }

    public int getWinCooldownSeconds() {
        return config.getInt("cooldown.win-cooldown-seconds", 0);
    }

    public int getRejoinCooldownSeconds() {
        return config.getInt("cooldown.rejoin-cooldown-seconds", 0);
    }

    public boolean isDisableTeleportInZone() {
        return config.getBoolean("anticheat.disable-teleport-in-zone", false);
    }

    public boolean isDisableFlightInZone() {
        return config.getBoolean("anticheat.disable-flight-in-zone", false);
    }

    public boolean isRemoveProgressOnQuit() {
        return config.getBoolean("anticheat.remove-progress-on-quit", true);
    }
}
