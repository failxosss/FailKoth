package net.kothplugin.scheduler;

import net.kothplugin.KothPlugin;
import net.kothplugin.koth.Koth;
import net.kothplugin.koth.KothState;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pokud je v config.yml povoleno autostart.enabled, tato třída periodicky
 * (v náhodném intervalu mezi min/max) automaticky spustí náhodný (nebo
 * postupný) KOTH bod, pokud právě žádný neběží.
 */
public class AutoStartScheduler {

    private final KothPlugin plugin;
    private final Random random = new Random();
    private BukkitTask task;
    private int roundRobinIndex = 0;

    public AutoStartScheduler(KothPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfigManager().isAutostartEnabled()) {
            return;
        }
        scheduleNext();
    }

    private void scheduleNext() {
        int min = Math.max(1, plugin.getConfigManager().getAutostartMinInterval());
        int max = Math.max(min, plugin.getConfigManager().getAutostartMaxInterval());
        int delayMinutes = min == max ? min : (min + random.nextInt(max - min + 1));
        long delayTicks = delayMinutes * 60L * 20L;

        task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            tryStartRandom();
            scheduleNext();
        }, delayTicks);
    }

    private void tryStartRandom() {
        boolean anyRunning = plugin.getKothManager().getAll().stream().anyMatch(k -> k.getState().isRunning());
        if (anyRunning) {
            return; // nepřekrývat eventy - počkej na další cyklus
        }

        List<Koth> candidates = new ArrayList<>();
        for (Koth koth : plugin.getKothManager().getAll()) {
            if (koth.isFullyConfigured() && koth.getState() == KothState.WAITING) {
                candidates.add(koth);
            }
        }
        if (candidates.isEmpty()) return;

        Koth chosen;
        if (plugin.getConfigManager().isAutostartRandom()) {
            chosen = candidates.get(random.nextInt(candidates.size()));
        } else {
            roundRobinIndex = roundRobinIndex % candidates.size();
            chosen = candidates.get(roundRobinIndex);
            roundRobinIndex++;
        }
        plugin.getKothManager().start(chosen);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
