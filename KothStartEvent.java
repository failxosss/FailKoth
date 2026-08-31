package net.kothplugin.events;

import net.kothplugin.koth.Koth;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Vyvolána ve chvíli, kdy KOTH event skutečně začne (po warmupu). */
public class KothStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Koth koth;

    public KothStartEvent(Koth koth) {
        this.koth = koth;
    }

    public Koth getKoth() {
        return koth;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
