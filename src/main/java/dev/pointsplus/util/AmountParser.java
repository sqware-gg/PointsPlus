package dev.pointsplus.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.OptionalLong;

public final class AmountParser {
    private AmountParser() {
    }

    public static OptionalLong parsePositive(String input) {
        if (input == null || input.isBlank()) {
            return OptionalLong.empty();
        }

        String normalized = input.trim()
                .replace(",", "")
                .replace("_", "")
                .toLowerCase(Locale.ROOT);
        long multiplier = multiplier(normalized);
        String number = multiplier == 1L ? normalized : normalized.substring(0, normalized.length() - 1);
        if (number.isBlank()) {
            return OptionalLong.empty();
        }

        try {
            BigDecimal amount = new BigDecimal(number).multiply(BigDecimal.valueOf(multiplier));
            if (amount.signum() <= 0 || amount.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
                return OptionalLong.empty();
            }
            BigDecimal whole = amount.setScale(0, RoundingMode.DOWN);
            if (whole.compareTo(BigDecimal.ONE) < 0) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(whole.longValueExact());
        } catch (ArithmeticException | NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }

    public static OptionalLong parseNonNegative(String input) {
        if (input == null || input.isBlank()) {
            return OptionalLong.empty();
        }
        String normalized = input.trim().replace(",", "").replace("_", "");
        if (normalized.equals("0")) {
            return OptionalLong.of(0L);
        }
        return parsePositive(input);
    }

    private static long multiplier(String normalized) {
        if (normalized.endsWith("k")) {
            return 1_000L;
        }
        if (normalized.endsWith("m")) {
            return 1_000_000L;
        }
        if (normalized.endsWith("b")) {
            return 1_000_000_000L;
        }
        if (normalized.endsWith("t")) {
            return 1_000_000_000_000L;
        }
        if (normalized.endsWith("q")) {
            return 1_000_000_000_000_000L;
        }
        return 1L;
    }
}
