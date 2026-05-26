package dev.pointsplus.api;

import dev.pointsplus.points.PointsService;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class PointsPlusApi {
    private static PointsService service;

    private PointsPlusApi() {
    }

    public static void register(PointsService pointsService) {
        service = pointsService;
    }

    public static void unregister() {
        service = null;
    }

    public static OptionalLong balance(UUID uuid) {
        return service == null ? OptionalLong.empty() : service.balance(uuid);
    }

    public static long balanceOrZero(UUID uuid) {
        return service == null ? 0L : service.balanceOrZero(uuid);
    }

    public static Optional<PointBalance> snapshot(UUID uuid) {
        return service == null ? Optional.empty() : service.snapshot(uuid);
    }

    public static Optional<PointBalance> snapshot(Player player) {
        return player == null ? Optional.empty() : snapshot(player.getUniqueId());
    }

    public static Optional<PointBalance> findByName(String name) {
        return service == null ? Optional.empty() : service.findByName(name);
    }

    public static List<PointBalance> top(int limit) {
        return service == null ? List.of() : service.top(limit);
    }

    public static int rank(UUID uuid) {
        return service == null ? -1 : service.rank(uuid);
    }

    public static boolean give(Player player, long amount) {
        if (service == null || player == null) {
            return false;
        }
        return service.give(player.getUniqueId(), player.getName(), amount, "API");
    }

    public static boolean take(Player player, long amount) {
        if (service == null || player == null) {
            return false;
        }
        return service.take(player.getUniqueId(), player.getName(), amount, "API");
    }

    public static boolean set(Player player, long balance) {
        if (service == null || player == null) {
            return false;
        }
        return service.set(player.getUniqueId(), player.getName(), balance, "API");
    }

    public static boolean pay(Player source, Player target, long amount) {
        if (service == null || source == null || target == null) {
            return false;
        }
        return service.pay(source.getUniqueId(), source.getName(), target.getUniqueId(), target.getName(), amount);
    }

    public static boolean pay(UUID sourceId, String sourceName, UUID targetId, String targetName, long amount) {
        if (service == null || sourceId == null || targetId == null) {
            return false;
        }
        return service.pay(sourceId, sourceName, targetId, targetName, amount);
    }
}
