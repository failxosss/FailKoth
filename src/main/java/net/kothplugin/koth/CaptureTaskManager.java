package net.kothplugin.koth;

import net.kothplugin.KothPlugin;
import net.kothplugin.events.KothCaptureProgressEvent;
import net.kothplugin.events.KothEndEvent;
import net.kothplugin.events.KothWinEvent;
import net.kothplugin.utils.TeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Řídí veškerou "živou" logiku KOTH eventů - warmup countdown, tikání capture
 * zóny, detekci kontestu, decay, notifikace (bossbar/actionbar/title/zvuky)
 * a vyhodnocení vítěze včetně odměn.
 */
public class CaptureTaskManager {

    private final KothPlugin plugin;
    private final Map<String, BukkitTask> warmupTasks = new HashMap<>();
    private final Map<String, BukkitTask> captureTasks = new HashMap<>();
    private final Map<String, BossBar> bossBars = new HashMap<>();

    public CaptureTaskManager(KothPlugin plugin) {
        this.plugin = plugin;
    }

    public void scheduleWarmup(Koth koth) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int left = koth.getWarmupSecondsLeft() - 1;
            koth.setWarmupSecondsLeft(left);
            if (left <= 0) {
                cancelWarmup(koth);
                plugin.getKothManager().activate(koth);
            } else if (left <= 5 || left % 10 == 0) {
                Map<String, String> ph = new HashMap<>();
                ph.put("name", koth.getName());
                ph.put("seconds", String.valueOf(left));
                ph.put("location", plugin.getKothManager().formatLocation(koth.getCapturePoint()));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(plugin.getLangManager().getPrefix(p) + plugin.getLangManager().get(p, "start.warmup-broadcast", ph));
                }
            }
        }, 20L, 20L);
        warmupTasks.put(koth.getName().toLowerCase(), task);
    }

    private void cancelWarmup(Koth koth) {
        BukkitTask task = warmupTasks.remove(koth.getName().toLowerCase());
        if (task != null) task.cancel();
    }

    public void scheduleCapture(Koth koth) {
        long interval = plugin.getConfigManager().getCheckIntervalTicks();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(koth, interval), interval, interval);
        captureTasks.put(koth.getName().toLowerCase(), task);

        if (plugin.getConfigManager().isBossbarEnabled()) {
            BossBar bar = Bukkit.createBossBar("", parseColor(plugin.getConfigManager().getBossbarColor()), parseStyle(plugin.getConfigManager().getBossbarStyle()));
            bossBars.put(koth.getName().toLowerCase(), bar);
        }
    }

    private BarColor parseColor(String s) {
        try {
            return BarColor.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.YELLOW;
        }
    }

    private BarStyle parseStyle(String s) {
        try {
            return BarStyle.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }

    /** Jeden "tik" capture logiky - volá se periodicky dle general.check-interval-ticks. */
    private void tick(Koth koth, long intervalTicks) {
        if (koth.getState() != KothState.ACTIVE && koth.getState() != KothState.CAPTURING) {
            return;
        }
        double secondsPerTick = intervalTicks / 20.0;
        boolean teamMode = plugin.getConfigManager().getTeamMode().equalsIgnoreCase("TEAM");

        // zjisti hráče aktuálně v zóně s právem koth.join
        List<Player> playersInZone = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("koth.join")) continue;
            if (koth.isPlayerInZone(p)) {
                playersInZone.add(p);
            }
        }

        Set<UUID> previouslyInZone = new HashSet<>(koth.getPlayersInZone().keySet());
        koth.getPlayersInZone().clear();
        boolean disableFlight = plugin.getConfigManager().isDisableFlightInZone();
        for (Player p : playersInZone) {
            koth.getPlayersInZone().put(p.getUniqueId(), System.currentTimeMillis());
            if (!previouslyInZone.contains(p.getUniqueId())) {
                plugin.getLangManager().send(p, "capture.entered-zone", Map.of("name", koth.getName()));
            }
            if (disableFlight && p.isFlying() && !p.hasPermission("koth.bypass.cooldown")) {
                p.setFlying(false);
                p.setAllowFlight(p.getGameMode() == org.bukkit.GameMode.CREATIVE);
            }
        }
        for (UUID left : previouslyInZone) {
            if (!koth.getPlayersInZone().containsKey(left)) {
                Player p = Bukkit.getPlayer(left);
                if (p != null) {
                    plugin.getLangManager().send(p, "capture.left-zone", Map.of("name", koth.getName()));
                }
            }
        }

        if (playersInZone.isEmpty()) {
            handleEmptyZone(koth, secondsPerTick);
            updateBossBarIdle(koth);
            plugin.getHologramManager().update(koth, "-", "-");
            return;
        }

        // rozdělení hráčů podle "vlastníka" (týmu, nebo jednotlivce v SOLO)
        String contestingGroups;
        if (teamMode) {
            Set<String> teams = new HashSet<>();
            for (Player p : playersInZone) {
                String team = TeamUtils.getTeamName(p);
                if (team == null && plugin.getConfigManager().isRequireTeam()) {
                    plugin.getLangManager().send(p, "capture.need-team");
                    continue;
                }
                teams.add(team != null ? team : p.getUniqueId().toString());
            }
            if (teams.isEmpty()) {
                handleEmptyZone(koth, secondsPerTick);
                updateBossBarIdle(koth);
                return;
            }
            if (teams.size() > 1 && plugin.getConfigManager().isContestEnabled()) {
                handleContest(koth, playersInZone);
                return;
            }
            String activeTeam = teams.iterator().next();
            advanceCapture(koth, playersInZone, secondsPerTick, activeTeam, true);
        } else {
            Set<UUID> uniquePlayers = new HashSet<>();
            for (Player p : playersInZone) uniquePlayers.add(p.getUniqueId());

            if (uniquePlayers.size() > 1 && plugin.getConfigManager().isContestEnabled()) {
                handleContest(koth, playersInZone);
                return;
            }
            advanceCapture(koth, playersInZone, secondsPerTick, null, false);
        }
    }

    private void handleContest(Koth koth, List<Player> playersInZone) {
        koth.setState(KothState.CAPTURING);
        String behaviour = plugin.getConfigManager().getContestBehaviour();
        if (behaviour.equalsIgnoreCase("reset")) {
            koth.setProgress(0);
        }
        for (Player p : playersInZone) {
            plugin.getLangManager().send(p, "capture.contested");
        }
        updateBossBarContested(koth);
        plugin.getHologramManager().update(koth, "Boj o bod!", "-");
    }

    private void handleEmptyZone(Koth koth, double secondsPerTick) {
        if (koth.getProgress() > 0 && plugin.getConfigManager().isDecayEnabled()) {
            double decay = plugin.getConfigManager().getDecayRatePerSecond() * secondsPerTick;
            koth.setProgress(koth.getProgress() - decay);
            if (koth.getState() == KothState.CAPTURING && koth.getProgress() <= 0) {
                koth.setState(KothState.ACTIVE);
                koth.setCurrentHolder(null);
                koth.setCurrentHolderTeam(null);
            }
        }
    }

    private void advanceCapture(Koth koth, List<Player> playersInZone, double secondsPerTick, String team, boolean teamMode) {
        koth.setState(KothState.CAPTURING);
        if (teamMode) {
            koth.setCurrentHolderTeam(team);
            koth.setCurrentHolder(null);
        } else {
            Player p = playersInZone.get(0);
            koth.setCurrentHolder(p.getUniqueId());
            koth.setCurrentHolderTeam(null);
        }

        // bonus rychlosti podle počtu hráčů (max do configu daného limitu)
        int boostCount = Math.min(playersInZone.size(), Math.max(1, plugin.getConfigManager().getMaxCaptureBoostPlayers() <= 0 ? playersInZone.size() : plugin.getConfigManager().getMaxCaptureBoostPlayers()));
        double multiplier = plugin.getConfigManager().getMultiCaptureMultiplier();
        double boostFactor = 1.0 + (boostCount - 1) * (multiplier - 1.0) / Math.max(1, plugin.getConfigManager().getMaxCaptureBoostPlayers());

        double captureTime = Math.max(1, koth.getCaptureTimeSeconds());
        double increment = (secondsPerTick / captureTime) * 100.0 * boostFactor;
        koth.setProgress(koth.getProgress() + increment);

        for (Player p : playersInZone) {
            Map<String, String> ph = new HashMap<>();
            ph.put("name", koth.getName());
            ph.put("progress", String.format("%.0f", koth.getProgress()));
            int remaining = (int) Math.ceil(((100 - koth.getProgress()) / 100.0) * captureTime);
            ph.put("time", String.valueOf(Math.max(0, remaining)));
            if (plugin.getConfigManager().isActionbarEnabled()) {
                p.sendActionBar(net.kyori.adventure.text.Component.text(
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getLangManager().get(p, "capture.progress-actionbar", ph))));
            }
            Bukkit.getPluginManager().callEvent(new KothCaptureProgressEvent(koth, p, koth.getProgress()));
        }
        updateBossBarProgress(koth, playersInZone);

        String holderLabel = teamMode ? team : playersInZone.get(0).getName();
        int remainingForHologram = (int) Math.ceil(((100 - koth.getProgress()) / 100.0) * captureTime);
        plugin.getHologramManager().update(koth, holderLabel, Math.max(0, remainingForHologram) + "s");

        if (plugin.getConfigManager().isSoundEnabled()) {
            for (Player p : playersInZone) {
                plugin.getKothManager().playSound(p, plugin.getConfigManager().getSound("capture-progress", "BLOCK_NOTE_BLOCK_HAT"));
            }
        }

        if (koth.getProgress() >= 100) {
            Player winner = teamMode ? playersInZone.get(0) : Bukkit.getPlayer(koth.getCurrentHolder());
            if (winner != null) {
                declareWin(koth, winner, team);
            }
        }
    }

    private void declareWin(Koth koth, Player winner, String team) {
        KothWinEvent event = new KothWinEvent(koth, winner);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            koth.setProgress(99);
            return;
        }

        koth.setState(KothState.CAPTURED);
        koth.setLastWinner(winner.getUniqueId());

        Map<String, String> ph = new HashMap<>();
        ph.put("name", koth.getName());
        ph.put("player", winner.getName());
        ph.put("team", team != null ? team : "");

        boolean teamMode = team != null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getConfigManager().isChatBroadcastEnabled()) {
                String path = teamMode ? "win.broadcast-team" : "win.broadcast";
                p.sendMessage(plugin.getLangManager().getPrefix(p) + plugin.getLangManager().get(p, path, ph));
            }
        }
        if (plugin.getConfigManager().isTitleEnabled()) {
            winner.sendTitle(plugin.getLangManager().get(winner, "win.title"),
                    plugin.getLangManager().get(winner, "win.subtitle", ph), 10, 60, 20);
        }
        plugin.getLangManager().send(winner, "win.personal-message", ph);
        if (plugin.getConfigManager().isSoundEnabled()) {
            plugin.getKothManager().playSound(winner, plugin.getConfigManager().getSound("capture-complete", "UI_TOTEM_USE"));
        }

        giveRewards(koth, winner, team);

        plugin.getStatsManager().recordWin(winner.getUniqueId(), winner.getName());
        removeBossBar(koth);
        plugin.getHologramManager().remove(koth);
        cancelCapture(koth);

        // krátká "ending" fáze, poté návrat do WAITING
        koth.setState(KothState.ENDING);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            koth.setState(KothState.WAITING);
            koth.resetRuntime();
            Bukkit.getPluginManager().callEvent(new KothEndEvent(koth, true));
        }, 100L);
    }

    private void giveRewards(Koth koth, Player winner, String team) {
        List<Player> recipients = new ArrayList<>();
        recipients.add(winner);
        if (team != null && plugin.getConfigManager().isGiveToWholeTeam()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(winner) && team.equals(TeamUtils.getTeamName(p)) && koth.getPlayersInZone().containsKey(p.getUniqueId())) {
                    recipients.add(p);
                }
            }
        }
        for (Player recipient : recipients) {
            for (String cmd : koth.getRewardCommands()) {
                String parsed = cmd.replace("%player%", recipient.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
            plugin.getLangManager().send(recipient, "win.rewards-given");
        }
    }

    private void updateBossBarProgress(Koth koth, List<Player> playersInZone) {
        BossBar bar = bossBars.get(koth.getName().toLowerCase());
        if (bar == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!bar.getPlayers().contains(p) && (koth.getPlayersInZone().containsKey(p.getUniqueId()))) {
                bar.addPlayer(p);
            }
        }
        // odeber hráče, kteří už nejsou v zóně
        List<Player> toRemove = new ArrayList<>();
        for (Player p : bar.getPlayers()) {
            if (!koth.getPlayersInZone().containsKey(p.getUniqueId())) {
                toRemove.add(p);
            }
        }
        toRemove.forEach(bar::removePlayer);

        bar.setProgress(Math.max(0, Math.min(1.0, koth.getProgress() / 100.0)));
        if (!playersInZone.isEmpty()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("name", koth.getName());
            ph.put("progress", String.format("%.0f", koth.getProgress()));
            bar.setTitle(plugin.getLangManager().get(playersInZone.get(0), "capture.progress-bossbar", ph));
        }
    }

    private void updateBossBarIdle(Koth koth) {
        BossBar bar = bossBars.get(koth.getName().toLowerCase());
        if (bar != null) {
            bar.setProgress(Math.max(0, Math.min(1.0, koth.getProgress() / 100.0)));
        }
    }

    private void updateBossBarContested(Koth koth) {
        BossBar bar = bossBars.get(koth.getName().toLowerCase());
        if (bar == null) return;
        for (var p : koth.getPlayersInZone().keySet()) {
            Player player = Bukkit.getPlayer(p);
            if (player != null) {
                bar.setTitle(plugin.getLangManager().get(player, "capture.contested"));
                break;
            }
        }
    }

    private void removeBossBar(Koth koth) {
        BossBar bar = bossBars.remove(koth.getName().toLowerCase());
        if (bar != null) {
            bar.removeAll();
        }
    }

    private void cancelCapture(Koth koth) {
        BukkitTask task = captureTasks.remove(koth.getName().toLowerCase());
        if (task != null) task.cancel();
    }

    /** Násilně zastaví jak warmup, tak capture tasky (např. /koth stop, mazání KOTH). */
    public void forceStop(Koth koth) {
        cancelWarmup(koth);
        cancelCapture(koth);
        removeBossBar(koth);
        plugin.getHologramManager().remove(koth);
    }

    public void shutdownAll() {
        for (BukkitTask task : warmupTasks.values()) task.cancel();
        for (BukkitTask task : captureTasks.values()) task.cancel();
        for (BossBar bar : bossBars.values()) bar.removeAll();
        warmupTasks.clear();
        captureTasks.clear();
        bossBars.clear();
    }
}
