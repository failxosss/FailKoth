package net.kothplugin.listeners;

import net.kothplugin.KothPlugin;
import net.kothplugin.koth.Koth;
import net.kothplugin.koth.KothState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Stará se o okrajové herní situace během běžícího KOTH:
 *  - odebrání hráče ze zóny při odpojení (proti AFK/exploit držení bodu)
 *  - volitelné blokování teleportačních příkazů/pluginů v capture zóně
 */
public class PlayerZoneListener implements Listener {

    private final KothPlugin plugin;

    public PlayerZoneListener(KothPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getConfigManager().isRemoveProgressOnQuit()) return;
        Player player = event.getPlayer();
        for (Koth koth : plugin.getKothManager().getAll()) {
            if (koth.getState() == KothState.CAPTURING && koth.getPlayersInZone().containsKey(player.getUniqueId())) {
                koth.getPlayersInZone().remove(player.getUniqueId());
                if (player.getUniqueId().equals(koth.getCurrentHolder())) {
                    koth.setCurrentHolder(null);
                }
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (!plugin.getConfigManager().isDisableTeleportInZone()) return;
        // typ COMMAND / PLUGIN teleportů (např. /tpa, enderpearl je typ ENDER_PEARL - to necháváme)
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND
                || event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            for (Koth koth : plugin.getKothManager().getAll()) {
                if (koth.getState().isRunning() && koth.getZone() != null && koth.getZone().contains(event.getFrom())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
