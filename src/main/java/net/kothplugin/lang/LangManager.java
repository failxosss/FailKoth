package net.kothplugin.lang;

import net.kothplugin.KothPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stará se o načítání a vydávání přeložených zpráv. Podporuje 20 vestavěných
 * jazyků (viz /lang/messages_<kod>.yml v resources) a umožňuje uživateli
 * přidat vlastní jazyk vytvořením nového souboru messages_<kod>.yml ve
 * složce pluginu.
 *
 * Jazyk se vybírá buď globálně (config.yml: language: "cs"), nebo
 * per-hráč, pokud je to povoleno (per-player-language.enabled: true) -
 * v tom případě se použije jazyk klienta hráče (Player#getLocale()),
 * pokud pro něj existuje odpovídající soubor, jinak fallback na výchozí jazyk.
 */
public class LangManager {

    /** Vestavěné jazyky dodávané s pluginem. */
    public static final Set<String> BUILTIN_LANGUAGES = new HashSet<>(List.of(
            "en", "cs", "sk", "de", "pl", "ru", "uk", "fr", "es", "pt",
            "it", "nl", "tr", "ro", "hu", "sv", "fi", "da", "no", "lt"
    ));

    private final KothPlugin plugin;
    private final Map<String, FileConfiguration> loadedLanguages = new HashMap<>();
    private String defaultLanguage;

    public LangManager(KothPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        loadedLanguages.clear();
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // Vygeneruje všechny vestavěné jazykové soubory do datové složky, pokud tam ještě nejsou
        for (String code : BUILTIN_LANGUAGES) {
            File target = new File(langFolder, "messages_" + code + ".yml");
            if (!target.exists()) {
                plugin.saveResource("lang/messages_" + code + ".yml", false);
            }
        }

        // Načte všechny .yml soubory ve složce lang (i případné vlastní jazyky uživatele)
        File[] files = langFolder.listFiles((dir, name) -> name.toLowerCase().startsWith("messages_") && name.toLowerCase().endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String code = file.getName().substring("messages_".length(), file.getName().length() - 4).toLowerCase();
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                // doplní chybějící klíče z vestavěného defaultu (angličtiny), pokud existuje
                InputStream defStream = plugin.getResource("lang/messages_" + code + ".yml");
                if (defStream != null) {
                    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
                    cfg.setDefaults(defConfig);
                }
                loadedLanguages.put(code, cfg);
            }
        }

        this.defaultLanguage = plugin.getConfigManager().getLanguage().toLowerCase();
        if (!loadedLanguages.containsKey(defaultLanguage)) {
            plugin.getLogger().warning("Jazyk '" + defaultLanguage + "' nebyl nalezen, používám angličtinu (en).");
            this.defaultLanguage = "en";
        }
    }

    public Set<String> getAvailableLanguages() {
        return loadedLanguages.keySet();
    }

    private FileConfiguration resolveConfig(CommandSender sender) {
        if (plugin.getConfigManager().isPerPlayerLanguage() && sender instanceof Player player) {
            String locale = player.locale().getLanguage().toLowerCase();
            if (loadedLanguages.containsKey(locale)) {
                return loadedLanguages.get(locale);
            }
        }
        return loadedLanguages.getOrDefault(defaultLanguage, loadedLanguages.get("en"));
    }

    /** Vrátí přeložený a obarvený text pro daný klíč (cesta oddělená tečkami, např. "start.success"). */
    public String get(CommandSender sender, String path) {
        FileConfiguration cfg = resolveConfig(sender);
        String raw = cfg != null ? cfg.getString(path) : null;
        if (raw == null) {
            return ChatColor.RED + "Missing lang key: " + path;
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /** Jako {@link #get(CommandSender, String)} ale s náhradou placeholderů %klic% -> hodnota. */
    public String get(CommandSender sender, String path, Map<String, String> placeholders) {
        String text = get(sender, path);
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                text = text.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return text;
    }

    public String getPrefix(CommandSender sender) {
        return get(sender, "prefix");
    }

    /** Odešle zprávu hráči/senderovi s automaticky předřazeným prefixem pluginu. */
    public void send(CommandSender sender, String path) {
        sender.sendMessage(getPrefix(sender) + get(sender, path));
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(getPrefix(sender) + get(sender, path, placeholders));
    }

    /** Odešle zprávu bez prefixu (např. pro víceřádkové výpisy). */
    public void sendRaw(CommandSender sender, String path) {
        sender.sendMessage(get(sender, path));
    }

    public void sendRaw(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(get(sender, path, placeholders));
    }

    /** Vrátí lokalizovaný název stavu KOTH bodu (např. "active" -> "Aktivní"). */
    public String getStateName(CommandSender sender, String stateKey) {
        return get(sender, "states." + stateKey.toLowerCase());
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }
}
