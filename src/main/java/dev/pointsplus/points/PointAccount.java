package dev.pointsplus.points;

import dev.pointsplus.api.PointBalance;
import java.util.UUID;

public final class PointAccount {
    private final UUID uuid;
    private String name = "";
    private long balance;
    private long createdAtMillis;
    private long updatedAtMillis;

    public PointAccount(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name == null ? "" : name;
    }

    public void name(String name) {
        if (name != null && !name.isBlank()) {
            this.name = cleanName(name);
        }
    }

    public long balance() {
        return balance;
    }

    public void balance(long balance) {
        this.balance = balance;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public long updatedAtMillis() {
        return updatedAtMillis;
    }

    public void load(String name, long balance, long createdAtMillis, long updatedAtMillis) {
        this.name = cleanName(name);
        this.balance = balance;
        this.createdAtMillis = Math.max(0L, createdAtMillis);
        this.updatedAtMillis = Math.max(0L, updatedAtMillis);
    }

    public void touch(long nowMillis) {
        if (createdAtMillis <= 0L) {
            createdAtMillis = nowMillis;
        }
        updatedAtMillis = nowMillis;
    }

    public PointBalance snapshot() {
        return new PointBalance(uuid, name(), balance, createdAtMillis, updatedAtMillis);
    }

    private String cleanName(String input) {
        return input == null ? "" : input.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
