package net.kothplugin.events;

import net.kothplugin.koth.Koth;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Vyvolána, když KOTH event skončí (ať už výhrou, nebo bez vítěze / vypršením). */
public class KothEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Koth koth;
    private final boolean hadWinner;

    public KothEndEvent(Koth koth, boolean hadWinner) {
        this.koth = koth;
        this.hadWinner = hadWinner;
    }

    public Koth getKoth() {
        return koth;
    }

    public boolean isHadWinner() {
        return hadWinner;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
