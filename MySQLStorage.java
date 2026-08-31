package net.kothplugin.storage;

import net.kothplugin.KothPlugin;
import net.kothplugin.stats.PlayerStats;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MySQL implementace úložiště - vhodné pro síť serverů (BungeeCord/Velocity),
 * kde chceš mít sdílené statistiky napříč všemi servery.
 *
 * Vyžaduje, aby byl na serveru dostupný JDBC ovladač pro MySQL (Paper jej
 * standardně obsahuje). Připojovací údaje se nastavují v config.yml
 * (storage.mysql.*).
 */
public class MySQLStorage implements StorageManager {

    private final KothPlugin plugin;
    private Connection connection;
    private String tablePrefix;

    public MySQLStorage(KothPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        this.tablePrefix = plugin.getConfigManager().getMysqlTablePrefix();
        try {
            String host = plugin.getConfigManager().getMysqlHost();
            int port = plugin.getConfigManager().getMysqlPort();
            String database = plugin.getConfigManager().getMysqlDatabase();
            String user = plugin.getConfigManager().getMysqlUsername();
            String pass = plugin.getConfigManager().getMysqlPassword();
            boolean ssl = plugin.getConfigManager().getMysqlUseSSL();

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=" + ssl + "&autoReconnect=true&characterEncoding=utf8";

            connection = DriverManager.getConnection(url, user, pass);

            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablePrefix + "stats (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "name VARCHAR(32) NOT NULL, " +
                        "wins INT NOT NULL DEFAULT 0, " +
                        "last_win BIGINT NOT NULL DEFAULT 0" +
                        ")");
            }
            plugin.getLogger().info("Připojeno k MySQL databázi (" + host + ":" + port + "/" + database + ").");
        } catch (SQLException e) {
            plugin.getLogger().severe("Nepodařilo se připojit k MySQL databázi: " + e.getMessage());
            plugin.getLogger().severe("Zkontroluj nastavení storage.mysql v config.yml. Plugin použije prázdné statistiky do dalšího restartu.");
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public PlayerStats getOrCreate(UUID uuid, String name) {
        if (connection == null) return new PlayerStats(uuid, name);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT wins, last_win FROM " + tablePrefix + "stats WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlayerStats stats = new PlayerStats(uuid, name);
                    stats.setWins(rs.getInt("wins"));
                    stats.setLastWinTimestamp(rs.getLong("last_win"));
                    return stats;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Chyba při čtení statistik z MySQL: " + e.getMessage());
        }
        PlayerStats fresh = new PlayerStats(uuid, name);
        save(fresh);
        return fresh;
    }

    @Override
    public void save(PlayerStats stats) {
        if (connection == null) return;
        String sql = "INSERT INTO " + tablePrefix + "stats (uuid, name, wins, last_win) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), wins = VALUES(wins), last_win = VALUES(last_win)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, stats.getUuid().toString());
            ps.setString(2, stats.getLastKnownName());
            ps.setInt(3, stats.getWins());
            ps.setLong(4, stats.getLastWinTimestamp());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Chyba při ukládání statistik do MySQL: " + e.getMessage());
        }
    }

    @Override
    public List<PlayerStats> getTop(int limit) {
        List<PlayerStats> result = new ArrayList<>();
        if (connection == null) return result;
        String sql = "SELECT uuid, name, wins, last_win FROM " + tablePrefix + "stats ORDER BY wins DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlayerStats stats = new PlayerStats(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                    stats.setWins(rs.getInt("wins"));
                    stats.setLastWinTimestamp(rs.getLong("last_win"));
                    result.add(stats);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Chyba při čtení TOP žebříčku z MySQL: " + e.getMessage());
        }
        return result;
    }

    @Override
    public int getRank(UUID uuid) {
        if (connection == null) return -1;
        String sql = "SELECT COUNT(*) + 1 AS rank FROM " + tablePrefix + "stats WHERE wins > (SELECT wins FROM " + tablePrefix + "stats WHERE uuid = ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rank");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Chyba při zjišťování pořadí z MySQL: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public void flush() {
        // MySQL zapisuje ihned při každém save(), není potřeba nic dalšího.
    }
}
