package dev.pointsplus.api;

import java.util.UUID;

public record PointBalance(UUID uuid, String name, long balance, long createdAtMillis, long updatedAtMillis) {
}
