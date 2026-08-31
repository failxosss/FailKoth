package net.kothplugin.koth;

import net.kothplugin.utils.Cuboid;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reprezentuje jeden KOTH bod (hill) na serveru - jeho konfiguraci i aktuální runtime stav.
 */
public class Koth {

    private final String name;
    private Cuboid zone;
    private Location capturePoint;

    private int captureTimeSeconds = 60;
    private int warmupTimeSeconds = 30;
    private final List<String> rewardCommands = new ArrayList<>();

    // runtime stav
    private KothState state = KothState.DISABLED;
    private double progress = 0.0; // 0-100
    private UUID currentHolder = null; // hráč/tým co aktuálně obsazuje
    private String currentHolderTeam = null;
    private UUID lastWinner = null;
    private long stateChangedAt = 0L;
    private int warmupSecondsLeft = 0;

    // hráči aktuálně stojící v zóně (uuid -> vstupní čas), pro kontrolu kontestu
    private final Map<UUID, Long> playersInZone = new LinkedHashMap<>();

    public Koth(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Cuboid getZone() {
        return zone;
    }

    public void setZone(Cuboid zone) {
        this.zone = zone;
    }

    public Location getCapturePoint() {
        return capturePoint;
    }

    public void setCapturePoint(Location capturePoint) {
        this.capturePoint = capturePoint;
    }

    public boolean isFullyConfigured() {
        return zone != null && capturePoint != null;
    }

    public int getCaptureTimeSeconds() {
        return captureTimeSeconds;
    }

    public void setCaptureTimeSeconds(int captureTimeSeconds) {
        this.captureTimeSeconds = captureTimeSeconds;
    }

    public int getWarmupTimeSeconds() {
        return warmupTimeSeconds;
    }

    public void setWarmupTimeSeconds(int warmupTimeSeconds) {
        this.warmupTimeSeconds = warmupTimeSeconds;
    }

    public List<String> getRewardCommands() {
        return rewardCommands;
    }

    public KothState getState() {
        return state;
    }

    public void setState(KothState state) {
        this.state = state;
        this.stateChangedAt = System.currentTimeMillis();
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = Math.max(0, Math.min(100, progress));
    }

    public UUID getCurrentHolder() {
        return currentHolder;
    }

    public void setCurrentHolder(UUID currentHolder) {
        this.currentHolder = currentHolder;
    }

    public String getCurrentHolderTeam() {
        return currentHolderTeam;
    }

    public void setCurrentHolderTeam(String currentHolderTeam) {
        this.currentHolderTeam = currentHolderTeam;
    }

    public UUID getLastWinner() {
        return lastWinner;
    }

    public void setLastWinner(UUID lastWinner) {
        this.lastWinner = lastWinner;
    }

    public int getWarmupSecondsLeft() {
        return warmupSecondsLeft;
    }

    public void setWarmupSecondsLeft(int warmupSecondsLeft) {
        this.warmupSecondsLeft = warmupSecondsLeft;
    }

    public Map<UUID, Long> getPlayersInZone() {
        return playersInZone;
    }

    public boolean isPlayerInZone(Player player) {
        if (zone == null) return false;
        return zone.contains(player.getLocation());
    }

    /** Resetuje runtime stav zpět na výchozí hodnoty (po ukončení eventu). */
    public void resetRuntime() {
        this.progress = 0;
        this.currentHolder = null;
        this.currentHolderTeam = null;
        this.playersInZone.clear();
        this.warmupSecondsLeft = 0;
    }

    public void saveTo(ConfigurationSection section) {
        if (zone != null) {
            zone.saveTo(section.createSection("zone"));
        }
        if (capturePoint != null) {
            section.set("capturePoint.world", capturePoint.getWorld().getName());
            section.set("capturePoint.x", capturePoint.getX());
            section.set("capturePoint.y", capturePoint.getY());
            section.set("capturePoint.z", capturePoint.getZ());
            section.set("capturePoint.yaw", capturePoint.getYaw());
            section.set("capturePoint.pitch", capturePoint.getPitch());
        }
        section.set("captureTimeSeconds", captureTimeSeconds);
        section.set("warmupTimeSeconds", warmupTimeSeconds);
        section.set("rewardCommands", rewardCommands);
    }

    public static Koth loadFrom(String name, ConfigurationSection section) {
        Koth koth = new Koth(name);
        if (section.isConfigurationSection("zone")) {
            koth.setZone(Cuboid.loadFrom(section.getConfigurationSection("zone")));
        }
        if (section.isConfigurationSection("capturePoint")) {
            ConfigurationSection cp = section.getConfigurationSection("capturePoint");
            org.bukkit.World w = org.bukkit.Bukkit.getWorld(cp.getString("world"));
            if (w != null) {
                Location loc = new Location(w, cp.getDouble("x"), cp.getDouble("y"), cp.getDouble("z"),
                        (float) cp.getDouble("yaw"), (float) cp.getDouble("pitch"));
                koth.setCapturePoint(loc);
            }
        }
        koth.setCaptureTimeSeconds(section.getInt("captureTimeSeconds", 60));
        koth.setWarmupTimeSeconds(section.getInt("warmupTimeSeconds", 30));
        List<String> rewards = section.getStringList("rewardCommands");
        if (rewards != null) {
            koth.getRewardCommands().addAll(rewards);
        }
        koth.setState(koth.isFullyConfigured() ? KothState.WAITING : KothState.DISABLED);
        return koth;
    }
}
