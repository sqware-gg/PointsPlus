package dev.pointsplus.event;

import dev.pointsplus.api.PointTransactionType;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PointsChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final String playerName;
    private final long oldBalance;
    private final long newBalance;
    private final long change;
    private final PointTransactionType transactionType;
    private final String actor;
    private boolean cancelled;

    public PointsChangeEvent(UUID playerId, String playerName, long oldBalance, long newBalance,
                             long change, PointTransactionType transactionType, String actor) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.playerName = playerName;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.change = change;
        this.transactionType = transactionType;
        this.actor = actor;
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public long oldBalance() {
        return oldBalance;
    }

    public long newBalance() {
        return newBalance;
    }

    public long change() {
        return change;
    }

    public PointTransactionType transactionType() {
        return transactionType;
    }

    public String actor() {
        return actor;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
