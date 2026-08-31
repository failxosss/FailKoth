package net.kothplugin.stats;

import java.util.UUID;

/**
 * Statistiky jednoho hráče - počet výher, poslední výhra atd.
 */
public class PlayerStats {

    private final UUID uuid;
    private String lastKnownName;
    private int wins;
    private long lastWinTimestamp;

    public PlayerStats(UUID uuid, String lastKnownName) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void incrementWins() {
        this.wins++;
        this.lastWinTimestamp = System.currentTimeMillis();
    }

    public long getLastWinTimestamp() {
        return lastWinTimestamp;
    }

    public void setLastWinTimestamp(long lastWinTimestamp) {
        this.lastWinTimestamp = lastWinTimestamp;
    }
}
