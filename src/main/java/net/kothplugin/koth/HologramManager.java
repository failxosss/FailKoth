package net.kothplugin.koth;

import net.kothplugin.KothPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spravuje jednoduchý "hologram" nad capture bodem KOTH - bez závislosti na
 * externím hologram pluginu (DecentHolograms apod.), pomocí neviditelných
 * ArmorStandů s vlastním jménem. Hologram má 3 řádky:
 *   1) název KOTH bodu
 *   2) status - kdo (nebo jaký tým) bod aktuálně obsazuje
 *   3) čas do konce (dokončení) capturu
 *
 * Zapíná/vypíná se configem integrations.holograms.enabled.
 */
public class HologramManager {

    private static final double LINE_SPACING = 0.28;
    private static final double BASE_HEIGHT_OFFSET = 2.0;

    private final KothPlugin plugin;
    private final Map<String, List<ArmorStand>> holograms = new HashMap<>();

    public HologramManager(KothPlugin plugin) {
        this.plugin = plugin;
    }

    /** Vytvoří hologram nad capture bodem daného KOTH (voláno při startu eventu). */
    public void spawn(Koth koth) {
        if (!plugin.getConfigManager().isHologramsEnabled()) return;
        Location point = koth.getCapturePoint();
        if (point == null || point.getWorld() == null) return;

        remove(koth); // pro jistotu, kdyby tam nějaký zbyl z minula

        Location base = point.clone().add(0.5, BASE_HEIGHT_OFFSET, 0.5);
        List<ArmorStand> lines = new ArrayList<>();

        lines.add(spawnLine(base.clone().add(0, LINE_SPACING * 2, 0),
                ChatColor.YELLOW + "" + ChatColor.BOLD + koth.getName()));
        lines.add(spawnLine(base.clone().add(0, LINE_SPACING, 0),
                ChatColor.GRAY + "Status: " + ChatColor.WHITE + "-"));
        lines.add(spawnLine(base,
                ChatColor.GRAY + "Konec za: " + ChatColor.WHITE + "-"));

        holograms.put(koth.getName().toLowerCase(), lines);
    }

    private ArmorStand spawnLine(Location location, String text) {
        return location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setCustomNameVisible(true);
            stand.setCustomName(text);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setPersistent(false);
            stand.setCollidable(false);
        });
    }

    /** Aktualizuje 2. řádek (status) a 3. řádek (zbývající čas) hologramu. */
    public void update(Koth koth, String statusText, String timeText) {
        if (!plugin.getConfigManager().isHologramsEnabled()) return;
        List<ArmorStand> lines = holograms.get(koth.getName().toLowerCase());
        if (lines == null || lines.size() < 3) return;

        ArmorStand status = lines.get(1);
        ArmorStand time = lines.get(2);
        if (status != null && status.isValid()) {
            status.setCustomName(ChatColor.GRAY + "Status: " + ChatColor.WHITE + statusText);
        }
        if (time != null && time.isValid()) {
            time.setCustomName(ChatColor.GRAY + "Konec za: " + ChatColor.WHITE + timeText);
        }
    }

    /** Smaže hologram daného KOTH (konec eventu, /koth stop, smazání bodu...). */
    public void remove(Koth koth) {
        List<ArmorStand> lines = holograms.remove(koth.getName().toLowerCase());
        if (lines == null) return;
        for (ArmorStand stand : lines) {
            if (stand != null && stand.isValid()) {
                stand.remove();
            }
        }
    }

    /** Smaže úplně všechny hologramy (např. při vypnutí pluginu). */
    public void removeAll() {
        for (List<ArmorStand> lines : holograms.values()) {
            for (ArmorStand stand : lines) {
                if (stand != null && stand.isValid()) {
                    stand.remove();
                }
            }
        }
        holograms.clear();
    }
}
