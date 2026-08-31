package net.kothplugin.listeners;

import net.kothplugin.KothPlugin;
import net.kothplugin.gui.KothGUI;
import net.kothplugin.koth.Koth;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;

public class GuiListener implements Listener {

    private final KothPlugin plugin;

    public GuiListener(KothPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof KothGUI.KothGuiHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String displayName = ChatColor.stripColor(meta.getDisplayName());

        Koth target = null;
        for (Koth koth : plugin.getKothManager().getAll()) {
            if (koth.getName().equalsIgnoreCase(displayName)) {
                target = koth;
                break;
            }
        }
        if (target == null || target.getCapturePoint() == null) return;

        player.closeInventory();
        player.teleport(target.getCapturePoint());
        if (plugin.getConfigManager().isGuiSoundsEnabled()) {
            plugin.getKothManager().playSound(player, "ENTITY_ENDERMAN_TELEPORT");
        }
    }
}
