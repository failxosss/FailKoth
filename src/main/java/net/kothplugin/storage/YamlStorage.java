package net.kothplugin.storage;

import net.kothplugin.KothPlugin;
import net.kothplugin.stats.PlayerStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Výchozí souborové (YAML) úložiště statistik - stats.yml v datové složce pluginu.
 * Nevyžaduje žádnou externí databázi, vhodné pro jednoduché / malé servery.
 */
public class YamlStorage implements StorageManager {

    private final KothPlugin plugin;
    private File file;
    private YamlConfiguration yaml;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();

    public YamlStorage(KothPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nepodařilo se vytvořit stats.yml: " + e.getMessage());
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        cache.clear();
        if (yaml.isConfigurationSection("players")) {
            ConfigurationSection playersSection = yaml.getConfigurationSection("players");
            for (String key : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    ConfigurationSection s = playersSection.getConfigurationSection(key);
                    PlayerStats stats = new PlayerStats(uuid, s.getString("name", "Unknown"));
                    stats.setWins(s.getInt("wins", 0));
                    stats.setLastWinTimestamp(s.getLong("lastWin", 0));
                    cache.put(uuid, stats);
                } catch (IllegalArgumentException ignored) {
                    // neplatný UUID klíč, přeskočit
                }
            }
        }
    }

    @Override
    public void close() {
        flush();
    }

    @Override
    public synchronized PlayerStats getOrCreate(UUID uuid, String name) {
        PlayerStats stats = cache.get(uuid);
        if (stats == null) {
            stats = new PlayerStats(uuid, name);
            cache.put(uuid, stats);
        } else {
            stats.setLastKnownName(name);
        }
        return stats;
    }

    @Override
    public synchronized void save(PlayerStats stats) {
        cache.put(stats.getUuid(), stats);
        writeToYaml(stats);
    }

    private void writeToYaml(PlayerStats stats) {
        String path = "players." + stats.getUuid();
        yaml.set(path + ".name", stats.getLastKnownName());
        yaml.set(path + ".wins", stats.getWins());
        yaml.set(path + ".lastWin", stats.getLastWinTimestamp());
    }

    @Override
    public synchronized List<PlayerStats> getTop(int limit) {
        List<PlayerStats> all = new ArrayList<>(cache.values());
        all.sort(Comparator.comparingInt(PlayerStats::getWins).reversed());
        if (all.size() > limit) {
            return all.subList(0, limit);
        }
        return all;
    }

    @Override
    public synchronized int getRank(UUID uuid) {
        List<PlayerStats> all = new ArrayList<>(cache.values());
        all.sort(Comparator.comparingInt(PlayerStats::getWins).reversed());
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getUuid().equals(uuid)) {
                return all.get(i).getWins() > 0 ? i + 1 : -1;
            }
        }
        return -1;
    }

    @Override
    public synchronized void flush() {
        if (yaml == null || file == null) return;
        for (PlayerStats stats : cache.values()) {
            writeToYaml(stats);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nepodařilo se uložit stats.yml: " + e.getMessage());
        }
    }
}
