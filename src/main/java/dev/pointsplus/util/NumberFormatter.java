package dev.pointsplus.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class NumberFormatter {
    private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final long[] SHORT_VALUES = {
            1_000_000_000_000_000L,
            1_000_000_000_000L,
            1_000_000_000L,
            1_000_000L,
            1_000L
    };
    private static final String[] SHORT_SUFFIXES = {"q", "t", "b", "m", "k"};

    private NumberFormatter() {
    }

    public static String group(long amount) {
        synchronized (INTEGER_FORMAT) {
            return INTEGER_FORMAT.format(amount);
        }
    }

    public static String shorthand(long amount) {
        long absolute = Math.abs(amount);
        for (int index = 0; index < SHORT_VALUES.length; index++) {
            long value = SHORT_VALUES[index];
            if (absolute >= value) {
                double shortened = amount / (double) value;
                String formatted = Math.abs(shortened) >= 100
                        ? String.format(Locale.US, "%.0f", shortened)
                        : String.format(Locale.US, "%.1f", shortened);
                if (formatted.endsWith(".0")) {
                    formatted = formatted.substring(0, formatted.length() - 2);
                }
                return formatted + SHORT_SUFFIXES[index];
            }
        }
        return Long.toString(amount);
    }
}
