package net.kothplugin.gui;

import net.kothplugin.KothPlugin;
import net.kothplugin.koth.Koth;
import net.kothplugin.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI menu se seznamem KOTH bodů - kliknutím se hráč teleportuje na vybraný bod
 * (pokud na to má právo a KOTH právě běží / nebo vždy, dle nastavení).
 */
public class KothGUI {

    public static final String INVENTORY_TITLE_KEY = "koth_gui_main";

    private final KothPlugin plugin;

    public KothGUI(KothPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getGuiTitle());
        int size = Math.max(9, Math.min(54, plugin.getConfigManager().getGuiSize()));
        Inventory inv = plugin.getServer().createInventory(new KothGuiHolder(), size, title);

        List<Koth> koths = new ArrayList<>(plugin.getKothManager().getAll());
        if (koths.isEmpty()) {
            inv.setItem(size / 2, new ItemBuilder(Material.BARRIER)
                    .name("&c" + plugin.getLangManager().get(player, "gui.no-koths"))
                    .build());
        } else {
            int slot = 0;
            for (Koth koth : koths) {
                if (slot >= size) break;
                inv.setItem(slot++, buildKothItem(player, koth));
            }
        }

        if (plugin.getConfigManager().isGuiSoundsEnabled()) {
            plugin.getKothManager().playSound(player, "UI_BUTTON_CLICK");
        }
        player.openInventory(inv);
    }

    private ItemStack buildKothItem(Player player, Koth koth) {
        Material material = switch (koth.getState()) {
            case ACTIVE, CAPTURING -> Material.LIME_WOOL;
            case WARMUP -> Material.YELLOW_WOOL;
            case CAPTURED, ENDING -> Material.ORANGE_WOOL;
            case WAITING -> Material.WHITE_WOOL;
            default -> Material.GRAY_WOOL;
        };

        Map<String, String> ph = Map.of("name", koth.getName());
        String stateText = plugin.getLangManager().getStateName(player, koth.getState().name());

        List<String> lore = new ArrayList<>();
        lore.add(plugin.getLangManager().get(player, "gui.item-lore-state", Map.of("state", stateText)));
        if (koth.getCapturePoint() != null) {
            lore.add(plugin.getLangManager().get(player, "gui.item-lore-world", Map.of("world", koth.getCapturePoint().getWorld().getName())));
        }
        lore.add("");
        lore.add(plugin.getLangManager().get(player, "gui.item-lore-click"));

        return new ItemBuilder(material)
                .name(plugin.getLangManager().get(player, "gui.item-name", ph))
                .lore(lore)
                .build();
    }

    /** Marker holder, aby šlo v listeneru bezpečně rozeznat, že se jedná o naše GUI. */
    public static class KothGuiHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
