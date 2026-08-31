package net.kothplugin.commands;

import net.kothplugin.KothPlugin;
import net.kothplugin.koth.Koth;
import net.kothplugin.koth.KothState;
import net.kothplugin.stats.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Zpracovává veškeré subpříkazy /koth ... a poskytuje tab-completion.
 */
public class KothCommand implements CommandExecutor, TabCompleter {

    private final KothPlugin plugin;
    private final Map<String, Long> pendingDeleteConfirm = new HashMap<>();

    public KothCommand(KothPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setpos1" -> handleSetPos(sender, args, true);
            case "setpos2" -> handleSetPos(sender, args, false);
            case "setcapturepoint" -> handleSetCapturePoint(sender, args);
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "tp" -> handleTeleport(sender, args);
            case "top" -> handleTop(sender);
            case "gui" -> handleGui(sender);
            case "reload" -> handleReload(sender);
            case "setwarmup" -> handleSetWarmup(sender, args);
            case "setcapturetime" -> handleSetCaptureTime(sender, args);
            case "setreward" -> handleSetReward(sender, args);
            case "setlang" -> handleSetLang(sender, args);
            default -> plugin.getLangManager().send(sender, "general.unknown-command");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.getLangManager().sendRaw(sender, "command.help-header");
        boolean admin = sender.hasPermission("koth.admin");
        List<String> keys = new ArrayList<>(List.of("help-list", "help-info", "help-tp", "help-top", "help-gui"));
        if (admin) {
            keys.addAll(List.of("help-create", "help-delete", "help-setpos1", "help-setpos2", "help-setcapturepoint",
                    "help-start", "help-stop", "help-reload", "help-setwarmup", "help-setcapturetime", "help-setreward", "help-setlang"));
        }
        for (String key : keys) {
            plugin.getLangManager().sendRaw(sender, "command." + key);
        }
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("koth.admin")) {
            plugin.getLangManager().send(sender, "general.no-permission");
            return false;
        }
        return true;
    }

    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getLangManager().send(sender, "general.player-only");
            return false;
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            plugin.getLangManager().send(sender, "create.usage");
            return;
        }
        String name = args[1];
        if (plugin.getKothManager().exists(name)) {
            plugin.getLangManager().send(sender, "general.koth-already-exists", Map.of("name", name));
            return;
        }
        plugin.getKothManager().create(name);
        plugin.getLangManager().send(sender, "create.success", Map.of("name", name));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            plugin.getLangManager().send(sender, "delete.usage");
            return;
        }
        String name = args[1];
        Koth koth = plugin.getKothManager().get(name);
        if (koth == null) {
            plugin.getLangManager().send(sender, "general.koth-not-found", Map.of("name", name));
            return;
        }
        String key = sender.getName() + ":" + name.toLowerCase();
        Long pending = pendingDeleteConfirm.get(key);
        if (pending != null && System.currentTimeMillis() - pending < 10_000) {
            pendingDeleteConfirm.remove(key);
            plugin.getKothManager().delete(name);
            plugin.getLangManager().send(sender, "delete.success", Map.of("name", name));
        } else {
            pendingDeleteConfirm.put(key, System.currentTimeMillis());
            plugin.getLangManager().send(sender, "delete.confirm");
        }
    }

    private void handleSetPos(CommandSender sender, String[] args, boolean pos1) {
        if (!requireAdmin(sender) || !requirePlayer(sender)) return;
        if (args.length < 2) {
            plugin.getLangManager().send(sender, pos1 ? "setpos.pos1-set" : "setpos.pos2-set");
            return;
        }
        Koth koth = getOrError(sender, args[1]);
        if (koth == null) return;
        Player player = (Player) sender;
        Location loc = player.getLocation();

        net.kothplugin.utils.Cuboid existingZone = koth.getZone();
        Location other = null;
        if (existingZone != null) {
            other = new Location(existingZone.bukkitWorld(), pos1 ? existingZone.getMaxX() : existingZone.getMinX(),
                    pos1 ? existingZone.getMaxY() : existingZone.getMinY(), pos1 ? existingZone.getMaxZ() : existingZone.getMinZ());
        }
        if (other == null) {
            // Pokud dosud nebyla zóna nastavena, použijeme stejný bod pro oba rohy (dokud se nenastaví druhý)
            other = loc;
        }
        net.kothplugin.utils.Cuboid newZone = net.kothplugin.utils.Cuboid.fromLocations(loc, other);
        koth.setZone(newZone);
        plugin.getKothManager().saveAll();
        plugin.getLangManager().send(sender, pos1 ? "setpos.pos1-set" : "setpos.pos2-set", Map.of("name", koth.getName()));
    }

    private void handleSetCapturePoint(CommandSender sender, String[] args) {
        if (!requireAdmin(sender) || !requirePlayer(sender)) return;
        if (args.length < 2) return;
        Koth koth = getOrError(sender, args[1]);
        if (koth == null) return;
        koth.setCapturePoint(((Player) sender).getLocation());
        if (koth.isFullyConfigured() && koth.getState() == KothState.DISABLED) {
            koth.setState(KothState.WAITING);
        }
        plugin.getKothManager().saveAll();
        plugin.getLangManager().send(sender, "setpos.capturepoint-set", Map.of("name", koth.getName()));
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            plugin.getLangManager().send(sender, "start.usage");
            return;
        }
        Koth koth = getOrError(sender, args[1]);
        if (koth == null) return;
        if (koth.getState().isRunning()) {
            plugin.getLangManager().send(sender, "start.already-running", Map.of("name", koth.getName()));
            return;
        }
        if (!koth.isFullyConfigured()) {
            plugin.getLangManager().send(sender, "start.missing-region");
            return;
        }
        plugin.getKothManager().start(koth);
        plugin.getLangManager().send(sender, "start.success", Map.of("name", koth.getName()));
    }

    private void handleStop(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            plugin.getLangManager().send(sender, "stop.usage");
            return;
        }
        Koth koth = getOrError(sender, args[1]);
        if (koth == null) return;
        if (!koth.getState().isRunning()) {
            plugin.getLangManager().send(sender, "stop.not-running", Map.of("name", koth.getName()));
            return;
        }
        plugin.getKothManager().stop(koth);
        plugin.getLangManager().send(sender, "stop.success", Map.of("name", koth.getName()));

        Map<String, String> ph = Map.of("name", koth.getName());
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getConfigManager().isChatBroadcastEnabled()) {
                p.sendMessage(plugin.getLangManager().getPrefix(p) + plugin.getLangManager().get(p, "stop.broadcast", ph));
            }
        }
    }

    private void handleList(CommandSender sender) {
        if (plugin.getKothManager().getAll().isEmpty()) {
            plugin.getLangManager().send(sender, "general.no-koths-found");
            return;
        }
        plugin.getLangManager().sendRaw(sender, "list.header");
        for (Koth koth : plugin.getKothManager().getAll()) {
            String state = plugin.getLangManager().getStateName(sender, koth.getState().name());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLangManager().get(sender, "list.entry", Map.of("name", koth.getName(), "state", state))));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) return;
        Koth koth = getOrError(sender, args[1]);
        if (koth == null) return;

        String stateText = plugin.getLangManager().getStateName(sender, koth.getState().name());
        String ownerText = "-";
        if (koth.getCurrentHolder() != null) {
            var op = plugin.getServer().getOfflinePlayer(koth.getCurrentHolder());
            ownerText = op.getName() != null ? op.getName() : "-";
        } else if (koth.getCurrentHolderTeam() != null) {
            ownerText = koth.getCurrentHolderTeam();
        }

        plugin.getLangManager().sendRaw(sender, "info.header", Map.of("name", koth.getName()));
        plugin.getLangManager().sendRaw(sender, "info.state", Map.of("state", stateText));
        if (koth.getCapturePoint() != null) {
            plugin.getLangManager().sendRaw(sender, "info.world", Map.of("world", koth.getCapturePoint().getWorld().getName()));
        }
        plugin.getLangManager().sendRaw(sender, "info.capture-time", Map.of("time", String.valueOf(koth.getCaptureTimeSeconds())));
        plugin.getLangManager().sendRaw(sender, "info.warmup-time", Map.of("time", String.valueOf(koth.getWarmupTimeSeconds())));
        plugin.getLangManager().sendRaw(sender, "info.owner", Map.of("owner", ownerText));
        plugin.getLangManager().sendRaw(sender, "info.rewards-count", Map.of("count", String.valueOf(koth.getRewardCommands().size())));
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return;
        if (args.length < 2) return;
        Koth koth = getOrError(sender, args[1]);
        if (koth == null || koth.getCapturePoint() == null) return;
        ((Player) sender).teleport(koth.getCapturePoint());
    }

    private void handleTop(CommandSender sender) {
        List<PlayerStats> top = plugin.getStatsManager().getTop(10);
        plugin.getLangManager().sendRaw(sender, "top.header");
        if (top.isEmpty()) {
            plugin.getLangManager().sendRaw(sender, "top.empty");
            return;
        }
        int rank = 1;
        for (PlayerStats stats : top) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLangManager().get(sender, "top.entry",
                    Map.of("rank", String.valueOf(rank), "player", stats.getLastKnownName(), "wins", String.valueOf(stats.getWins())))));
            rank++;
        }
        if (sender instanceof Player player) {
            int myRank = plugin.getStatsManager().getRank(player.getUniqueId());
            if (myRank > 0) {
                PlayerStats myStats = plugin.getStatsManager().getStats(player.getUniqueId(), player.getName());
                plugin.getLangManager().sendRaw(sender, "top.your-rank",
                        Map.of("rank", String.valueOf(myRank), "wins", String.valueOf(myStats.getWins())));
            }
        }
    }

    private void handleGui(CommandSender sender) {
        if (!requirePlayer(sender)) return;
        if (!plugin.getConfigManager().isGuiEnabled()) {
            plugin.getLangManager().send(sender, "general.feature-disabled");
            return;
        }
        plugin.getKothGUI().open((Player) sender);
    }

    private void handleReload(CommandSender sender) {
        if (!requireAdmin(sender)) return;
        plugin.getConfigManager().reload();
        plugin.getLangManager().reload();
        plugin.getKothManager().load();
        plugin.getLangManager().send(sender, "general.reload-success");
    }

    private void handleSetWarmup(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) return;
        Koth koth = getOrError(sender, args[1]);
        if (koth == null) return;
        try {
            int seconds = Integer.parseInt(args[2]);
            koth.setWarmupTimeSeconds(seconds);
            plugin.getKothManager().saveAll();
            sender.sendMessage(plugin.getLangManager().getPrefix(sender) + "OK");
        } catch (NumberFormatException e) {
            plugin.getLangManager().send(sender, "general.invalid-number");
        }
    }

    private void handleSetCaptureTime(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) return;
        Koth koth = getOrError(sender, args[1]);
        if (koth == null) return;
        try {
            int seconds = Integer.parseInt(args[2]);
            koth.setCaptureTimeSeconds(seconds);
            plugin.getKothManager().saveAll();
            sender.sendMessage(plugin.getLangManager().getPrefix(sender) + "OK");
        } catch (NumberFormatException e) {
            plugin.getLangManager().send(sender, "general.invalid-number");
        }
    }

    private void handleSetReward(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            plugin.getLangManager().send(sender, "reward.usage-add");
            return;
        }
        Koth koth = getOrError(sender, args[1]);
        if (koth == null) return;
        String cmd = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        koth.getRewardCommands().add(cmd);
        plugin.getKothManager().saveAll();
        plugin.getLangManager().send(sender, "reward.added", Map.of("name", koth.getName()));
    }

    private void handleSetLang(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            plugin.getLangManager().send(sender, "lang.usage");
            return;
        }
        String code = args[1].toLowerCase();
        if (!plugin.getLangManager().getAvailableLanguages().contains(code)) {
            plugin.getLangManager().send(sender, "lang.invalid", Map.of(
                    "lang", code, "available", String.join(", ", plugin.getLangManager().getAvailableLanguages())));
            return;
        }
        plugin.getConfigManager().raw().set("language", code);
        plugin.saveConfig();
        plugin.getConfigManager().reload();
        plugin.getLangManager().reload();
        plugin.getLangManager().send(sender, "lang.changed", Map.of("lang", code));
    }

    private Koth getOrError(CommandSender sender, String name) {
        Koth koth = plugin.getKothManager().get(name);
        if (koth == null) {
            plugin.getLangManager().send(sender, "general.koth-not-found", Map.of("name", name));
        }
        return koth;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("help", "list", "info", "tp", "top", "gui"));
            if (sender.hasPermission("koth.admin")) {
                subs.addAll(List.of("create", "delete", "setpos1", "setpos2", "setcapturepoint", "start", "stop",
                        "reload", "setwarmup", "setcapturetime", "setreward", "setlang"));
            }
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            List<String> namedSubs = List.of("delete", "setpos1", "setpos2", "setcapturepoint", "start", "stop",
                    "info", "tp", "setwarmup", "setcapturetime", "setreward");
            if (namedSubs.contains(args[0].toLowerCase())) {
                List<String> names = plugin.getKothManager().getAll().stream().map(Koth::getName).collect(Collectors.toList());
                return filter(names, args[1]);
            }
            if (args[0].equalsIgnoreCase("setlang")) {
                return filter(new ArrayList<>(plugin.getLangManager().getAvailableLanguages()), args[1]);
            }
        }
        return options;
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
