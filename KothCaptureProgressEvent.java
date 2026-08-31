package net.kothplugin.events;

import net.kothplugin.koth.Koth;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Vyvolána při každé aktualizaci progresu capturu (přibližně jednou za check-interval). */
public class KothCaptureProgressEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Koth koth;
    private final Player capturingPlayer;
    private final double progress;

    public KothCaptureProgressEvent(Koth koth, Player capturingPlayer, double progress) {
        this.koth = koth;
        this.capturingPlayer = capturingPlayer;
        this.progress = progress;
    }

    public Koth getKoth() {
        return koth;
    }

    public Player getCapturingPlayer() {
        return capturingPlayer;
    }

    public double getProgress() {
        return progress;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
