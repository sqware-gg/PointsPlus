package dev.pointsplus.config;

import dev.pointsplus.util.NumberFormatter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PointsPlusConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public PointsPlusConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public long saveIntervalTicks() {
        return Math.max(20L, seconds("storage.save-interval-seconds", 300L) * 20L);
    }

    public boolean saveOnMutation() {
        return config.getBoolean("storage.save-on-mutation", true);
    }

    public int leaderboardPageSize() {
        return Math.max(1, config.getInt("storage.leaderboard-page-size", 10));
    }

    public boolean createOnAdminWrite() {
        return config.getBoolean("accounts.create-on-admin-write", true);
    }

    public boolean createPayTargets() {
        return config.getBoolean("accounts.create-pay-targets", false);
    }

    public boolean allowNegativeBalances() {
        return config.getBoolean("economy.allow-negative-balances", false);
    }

    public long maxBalance() {
        return Math.max(0L, config.getLong("economy.max-balance", Long.MAX_VALUE));
    }

    public String singularName() {
        return config.getString("economy.singular-name", "point");
    }

    public String pluralName() {
        return config.getString("economy.plural-name", "points");
    }

    public String symbol() {
        return config.getString("economy.symbol", "");
    }

    public boolean paymentsEnabled() {
        return config.getBoolean("payments.enabled", true);
    }

    public long minimumPayment() {
        return Math.max(1L, config.getLong("payments.minimum", 1L));
    }

    public long maximumPayment() {
        return Math.max(minimumPayment(), config.getLong("payments.maximum", Long.MAX_VALUE));
    }

    public boolean notifyPaymentTarget() {
        return config.getBoolean("payments.notify-target", true);
    }

    public boolean placeholdersEnabled() {
        return config.getBoolean("placeholders.enabled", true);
    }

    public boolean playerPointsPlaceholdersEnabled() {
        return config.getBoolean("placeholders.playerpoints-compatibility", true);
    }

    public String prefix() {
        return message("prefix");
    }

    public String message(String key) {
        return config.getString("messages." + key, "");
    }

    public String formatBalance(long amount) {
        String symbolPrefix = symbol();
        if (symbolPrefix == null) {
            symbolPrefix = "";
        }
        String number = symbolPrefix.isBlank() ? NumberFormatter.group(amount) : symbolPrefix + NumberFormatter.group(amount);
        String unit = Math.abs(amount) == 1L ? singularName() : pluralName();
        return number + " " + unit;
    }

    private long seconds(String path, long fallback) {
        return Math.max(0L, config.getLong(path, fallback));
    }
}
