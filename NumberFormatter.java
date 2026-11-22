package net.donutsim.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public final class NumberFormatter {
    private static final DecimalFormat COMPACT_FORMAT = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final NumberFormat INT_FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    private NumberFormatter() {}

    /**
     * Parses amounts supporting standard DonutSMP suffixes: K, M, B, T (case-insensitive)
     * as well as optional leading '$' and commas.
     */
    public static double parseAmount(String raw) throws NumberFormatException {
        if (raw == null) {
            throw new NumberFormatException("Empty amount");
        }
        String cleaned = raw.trim().replace("$", "").replace(",", "").toUpperCase(Locale.ROOT);
        if (cleaned.isEmpty()) {
            throw new NumberFormatException("Empty amount");
        }

        double multiplier = 1.0;
        char last = cleaned.charAt(cleaned.length() - 1);
        if (Character.isLetter(last)) {
            switch (last) {
                case 'K' -> multiplier = 1_000.0;
                case 'M' -> multiplier = 1_000_000.0;
                case 'B' -> multiplier = 1_000_000_000.0;
                case 'T' -> multiplier = 1_000_000_000_000.0;
                default -> throw new NumberFormatException("Unknown suffix: " + last);
            }
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }

        double base = Double.parseDouble(cleaned);
        if (Double.isNaN(base) || Double.isInfinite(base) || base < 0) {
            throw new NumberFormatException("Invalid numeric value");
        }
        return base * multiplier;
    }

    /**
     * Formats currency values using DonutSMP compact suffixes (K, M, B, T).
     */
    public static String formatCompact(double value) {
        double abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (abs >= 1_000_000_000_000.0) {
            return sign + COMPACT_FORMAT.format(abs / 1_000_000_000_000.0) + "T";
        } else if (abs >= 1_000_000_000.0) {
            return sign + COMPACT_FORMAT.format(abs / 1_000_000_000.0) + "B";
        } else if (abs >= 1_000_000.0) {
            return sign + COMPACT_FORMAT.format(abs / 1_000_000.0) + "M";
        } else if (abs >= 1_000.0) {
            return sign + COMPACT_FORMAT.format(abs / 1_000.0) + "K";
        }
        return sign + COMPACT_FORMAT.format(abs);
    }

    public static String formatWhole(long value) {
        return INT_FORMAT.format(value);
    }

    public static String formatTimer(int totalSeconds) {
        int clamped = Math.max(0, totalSeconds);
        int minutes = clamped / 60;
        int seconds = clamped % 60;
        return String.format(Locale.US, "%dm %02ds", minutes, seconds);
    }
}
