package dev.pointsplus.hook;

import dev.pointsplus.api.PointBalance;
import dev.pointsplus.points.PointsService;
import dev.pointsplus.util.NumberFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaceholderApiExpansion extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final PointsService pointsService;
    private final String identifier;

    public PlaceholderApiExpansion(JavaPlugin plugin, PointsService pointsService, String identifier) {
        this.plugin = plugin;
        this.pointsService = pointsService;
        this.identifier = identifier;
    }

    @Override
    public String getAuthor() {
        return "SQWARE / Conflict";
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return "";
        }

        String key = params.toLowerCase(Locale.ROOT);
        if (key.startsWith("leaderboard_")) {
            return leaderboard(key);
        }
        if (key.startsWith("points_formatted_for_")) {
            return format(pointsForName(params.substring("points_formatted_for_".length())));
        }
        if (key.startsWith("points_shorthand_for_")) {
            return NumberFormatter.shorthand(pointsForName(params.substring("points_shorthand_for_".length())));
        }
        if (key.startsWith("points_for_")) {
            return Long.toString(pointsForName(params.substring("points_for_".length())));
        }
        if (key.startsWith("balance_formatted_for_")) {
            return format(pointsForName(params.substring("balance_formatted_for_".length())));
        }
        if (key.startsWith("balance_shorthand_for_")) {
            return NumberFormatter.shorthand(pointsForName(params.substring("balance_shorthand_for_".length())));
        }
        if (key.startsWith("balance_for_")) {
            return Long.toString(pointsForName(params.substring("balance_for_".length())));
        }

        long balance = player == null ? 0L : pointsService.balanceOrZero(player.getUniqueId());
        return switch (key) {
            case "points", "balance" -> Long.toString(balance);
            case "points_formatted", "balance_formatted" -> format(balance);
            case "points_shorthand", "balance_shorthand" -> NumberFormatter.shorthand(balance);
            case "leaderboard_position", "rank" -> rank(player, false);
            case "leaderboard_position_formatted", "rank_formatted" -> rank(player, true);
            default -> null;
        };
    }

    private String leaderboard(String key) {
        String rest = key.substring("leaderboard_".length());
        String[] pieces = rest.split("_", 2);
        int position;
        try {
            position = Integer.parseInt(pieces[0]);
        } catch (NumberFormatException ignored) {
            return "";
        }
        if (position <= 0) {
            return "";
        }

        List<PointBalance> top = pointsService.top(position);
        if (top.size() < position) {
            return "";
        }
        PointBalance balance = top.get(position - 1);
        if (pieces.length == 1) {
            return balance.name();
        }
        return switch (pieces[1]) {
            case "amount", "points", "balance" -> Long.toString(balance.balance());
            case "amount_formatted", "points_formatted", "balance_formatted" -> format(balance.balance());
            case "amount_shorthand", "points_shorthand", "balance_shorthand" -> NumberFormatter.shorthand(balance.balance());
            default -> "";
        };
    }

    private long pointsForName(String playerName) {
        Optional<PointBalance> balance = pointsService.findByName(playerName);
        return balance.map(PointBalance::balance).orElse(0L);
    }

    private String rank(OfflinePlayer player, boolean formatted) {
        if (player == null) {
            return "";
        }
        int rank = pointsService.rank(player.getUniqueId());
        if (rank <= 0) {
            return "";
        }
        return formatted ? NumberFormatter.group(rank) : Integer.toString(rank);
    }

    private String format(long amount) {
        return NumberFormatter.group(amount);
    }
}
