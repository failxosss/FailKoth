package net.kothplugin.stats;

import net.kothplugin.KothPlugin;
import net.kothplugin.storage.MySQLStorage;
import net.kothplugin.storage.StorageManager;
import net.kothplugin.storage.YamlStorage;

import java.util.List;
import java.util.UUID;

/**
 * Vysokoúrovňové API pro práci se statistikami hráčů. Vybírá vhodnou implementaci
 * {@link StorageManager} podle nastavení storage.type v config.yml (YAML/MYSQL).
 */
public class StatsManager {

    private final KothPlugin plugin;
    private StorageManager storage;

    public StatsManager(KothPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        String type = plugin.getConfigManager().getStorageType();
        if (type.equalsIgnoreCase("MYSQL")) {
            storage = new MySQLStorage(plugin);
        } else {
            storage = new YamlStorage(plugin);
        }
        storage.init();
    }

    public void shutdown() {
        if (storage != null) {
            storage.close();
        }
    }

    public void recordWin(UUID uuid, String name) {
        PlayerStats stats = storage.getOrCreate(uuid, name);
        stats.incrementWins();
        storage.save(stats);
    }

    public PlayerStats getStats(UUID uuid, String name) {
        return storage.getOrCreate(uuid, name);
    }

    public List<PlayerStats> getTop(int limit) {
        return storage.getTop(limit);
    }

    public int getRank(UUID uuid) {
        return storage.getRank(uuid);
    }

    public void flush() {
        if (storage != null) {
            storage.flush();
        }
    }
}
