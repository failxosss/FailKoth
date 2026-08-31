package net.kothplugin.hooks;

import net.kothplugin.KothPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Volitelná integrace s Vault ekonomikou - umožňuje udílet peněžní odměny vítězům.
 * Pokud Vault nebo žádný ekonomický plugin není nainstalovaný, hook zůstane neaktivní
 * a veškeré metody bezpečně vrátí false/no-op.
 */
public class VaultHook {

    private final KothPlugin plugin;
    private Economy economy;
    private boolean enabled = false;

    public VaultHook(KothPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (!plugin.getConfigManager().isVaultEnabled()) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        economy = rsp.getProvider();
        enabled = true;
        plugin.getLogger().info("Vault ekonomika byla úspěšně napojena.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (!enabled || economy == null || amount <= 0) return;
        economy.depositPlayer(player, amount);
    }
}
