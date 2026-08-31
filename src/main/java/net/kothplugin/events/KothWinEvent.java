package net.kothplugin.events;

import net.kothplugin.koth.Koth;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Vyvolána ve chvíli, kdy hráč/tým dokončí capture a vyhraje KOTH.
 * Lze zrušit (cancel) - v takovém případě se odměny neudělí a capture pokračuje dál.
 */
public class KothWinEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Koth koth;
    private final Player winner;
    private boolean cancelled = false;

    public KothWinEvent(Koth koth, Player winner) {
        this.koth = koth;
        this.winner = winner;
    }

    public Koth getKoth() {
        return koth;
    }

    public Player getWinner() {
        return winner;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
