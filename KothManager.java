package net.kothplugin.koth;

import net.kothplugin.KothPlugin;
import net.kothplugin.events.KothEndEvent;
import net.kothplugin.events.KothStartEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Centrální správce všech KOTH bodů - stará se o jejich vytváření, mazání,
 * ukládání do koths.yml a spouštění/zastavování eventů.
 */
public class KothManager {

    private final KothPlugin plugin;
    private final Map<String, Koth> koths = new HashMap<>();
    private File file;
    private YamlConfiguration yaml;

    public KothManager(KothPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "koths.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nepodařilo se vytvořit koths.yml: " + e.getMessage());
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        koths.clear();
        if (yaml.isConfigurationSection("koths")) {
            ConfigurationSection section = yaml.getConfigurationSection("koths");
            for (String name : section.getKeys(false)) {
                Koth koth = Koth.loadFrom(name, section.getConfigurationSection(name));
                koths.put(name.toLowerCase(), koth);
            }
        }
        plugin.getLogger().info("Načteno " + koths.size() + " KOTH bodů.");
    }

    public void saveAll() {
        if (yaml == null) return;
        yaml.set("koths", null); // vyčistit staré
        for (Koth koth : koths.values()) {
            koth.saveTo(yaml.createSection("koths." + koth.getName()));
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nepodařilo se uložit koths.yml: " + e.getMessage());
        }
    }

    public Collection<Koth> getAll() {
        return koths.values();
    }

    public Koth get(String name) {
        if (name == null) return null;
        return koths.get(name.toLowerCase());
    }

    public boolean exists(String name) {
        return koths.containsKey(name.toLowerCase());
    }

    public Koth create(String name) {
        Koth koth = new Koth(name);
        koths.put(name.toLowerCase(), koth);
        saveAll();
        return koth;
    }

    public void delete(String name) {
        Koth koth = get(name);
        if (koth != null && koth.getState().isRunning()) {
            plugin.getCaptureTaskManager().forceStop(koth);
        }
        koths.remove(name.toLowerCase());
        saveAll();
    }

    /**
     * Spustí KOTH event. Pokud je warmup povolený, nejprve proběhne odpočet,
     * teprve poté se bod stane obsaditelným a je vyvolán {@link KothStartEvent}.
     */
    public boolean start(Koth koth) {
        if (koth == null || !koth.isFullyConfigured() || koth.getState().isRunning()) {
            return false;
        }
        koth.resetRuntime();

        boolean warmup = plugin.getConfigManager().isWarmupEnabled() && koth.getWarmupTimeSeconds() > 0;
        if (warmup) {
            koth.setState(KothState.WARMUP);
            koth.setWarmupSecondsLeft(koth.getWarmupTimeSeconds());
            broadcastWarmup(koth);
            plugin.getCaptureTaskManager().scheduleWarmup(koth);
        } else {
            activate(koth);
        }
        return true;
    }

    /** Interně volané po doběhnutí warmupu (nebo okamžitě, pokud warmup je vypnutý). */
    public void activate(Koth koth) {
        koth.setState(KothState.ACTIVE);
        Bukkit.getPluginManager().callEvent(new KothStartEvent(koth));
        broadcastStart(koth);
        plugin.getCaptureTaskManager().scheduleCapture(koth);
    }

    public boolean stop(Koth koth) {
        if (koth == null || !koth.getState().isRunning()) {
            return false;
        }
        plugin.getCaptureTaskManager().forceStop(koth);
        koth.setState(KothState.WAITING);
        koth.resetRuntime();
        Bukkit.getPluginManager().callEvent(new KothEndEvent(koth, false));
        return true;
    }

    private void broadcastWarmup(Koth koth) {
        if (!plugin.getConfigManager().isChatBroadcastEnabled()) return;
        Map<String, String> ph = new HashMap<>();
        ph.put("name", koth.getName());
        ph.put("seconds", String.valueOf(koth.getWarmupTimeSeconds()));
        ph.put("location", formatLocation(koth.getCapturePoint()));
        for (var player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(plugin.getLangManager().getPrefix(player) + plugin.getLangManager().get(player, "start.warmup-broadcast", ph));
        }
    }

    private void broadcastStart(Koth koth) {
        Map<String, String> ph = new HashMap<>();
        ph.put("name", koth.getName());
        for (var player : Bukkit.getOnlinePlayers()) {
            if (plugin.getConfigManager().isChatBroadcastEnabled()) {
                player.sendMessage(plugin.getLangManager().getPrefix(player) + plugin.getLangManager().get(player, "start.begin-broadcast", ph));
            }
            if (plugin.getConfigManager().isTitleEnabled()) {
                String title = plugin.getLangManager().get(player, "start.begin-title");
                String subtitle = plugin.getLangManager().get(player, "start.begin-subtitle", ph);
                player.sendTitle(title, subtitle, 10, 60, 20);
            }
            if (plugin.getConfigManager().isSoundEnabled()) {
                playSound(player, plugin.getConfigManager().getSound("koth-start", "ENTITY_ENDER_DRAGON_GROWL"));
            }
        }
    }

    public void playSound(org.bukkit.entity.Player player, String soundName) {
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ignored) {
            // neplatný název zvuku v configu - ignorovat
        }
    }

    public String formatLocation(Location loc) {
        if (loc == null) return "?";
        return loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}
