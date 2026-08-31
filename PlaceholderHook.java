package net.kothplugin.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kothplugin.KothPlugin;
import net.kothplugin.koth.Koth;
import net.kothplugin.stats.PlayerStats;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * PlaceholderAPI expanze - registruje placeholdery ve tvaru %koth_...%.
 *
 * Dostupné placeholdery:
 *  %koth_wins%                       - počet výher hráče (celkem)
 *  %koth_rank%                       - pořadí hráče v žebříčku
 *  %koth_active_count%                - počet aktuálně běžících KOTH eventů
 *  %koth_state_<název>%               - stav konkrétního KOTH bodu (lokalizovaný)
 *  %koth_progress_<název>%            - aktuální progres capturu (0-100)
 *  %koth_owner_<název>%               - jméno aktuálního držitele bodu
 */
public class PlaceholderHook extends PlaceholderExpansion {

    private final KothPlugin plugin;

    public PlaceholderHook(KothPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "koth";
    }

    @Override
    public @NotNull String getAuthor() {
        return "KothPlugin Team";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(org.bukkit.entity.Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("active_count")) {
            long count = plugin.getKothManager().getAll().stream().filter(k -> k.getState().isRunning()).count();
            return String.valueOf(count);
        }

        if (player != null) {
            if (params.equalsIgnoreCase("wins")) {
                PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId(), player.getName());
                return String.valueOf(stats.getWins());
            }
            if (params.equalsIgnoreCase("rank")) {
                int rank = plugin.getStatsManager().getRank(player.getUniqueId());
                return rank > 0 ? String.valueOf(rank) : "-";
            }
        }

        if (params.startsWith("state_")) {
            String name = params.substring("state_".length());
            Koth koth = plugin.getKothManager().get(name);
            if (koth == null) return "N/A";
            return koth.getState().name();
        }

        if (params.startsWith("progress_")) {
            String name = params.substring("progress_".length());
            Koth koth = plugin.getKothManager().get(name);
            if (koth == null) return "0";
            return String.format("%.0f", koth.getProgress());
        }

        if (params.startsWith("owner_")) {
            String name = params.substring("owner_".length());
            Koth koth = plugin.getKothManager().get(name);
            if (koth == null || koth.getCurrentHolder() == null) return "-";
            OfflinePlayer op = plugin.getServer().getOfflinePlayer(koth.getCurrentHolder());
            return op.getName() != null ? op.getName() : "-";
        }

        return null;
    }
}
