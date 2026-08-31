package net.kothplugin.utils;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/**
 * Pomocné metody pro zjištění týmové příslušnosti hráče přes vestavěný
 * Bukkit Scoreboard systém (/team add, /team join apod.). Díky tomu plugin
 * nevyžaduje žádný externí "teams" plugin pro týmový KOTH režim.
 */
public class TeamUtils {

    private TeamUtils() {}

    /** Vrátí název týmu hráče (dle hlavního serverového scoreboardu), nebo null pokud v žádném není. */
    public static String getTeamName(Player player) {
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        return team != null ? team.getName() : null;
    }
}
