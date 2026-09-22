package com.vaultdesk.admin; // use matching package name in the employee app copy

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeFormatUtil {

    private static final DateTimeFormatter STORED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");
    private static final DateTimeFormatter DISPLAY_DATE_ONLY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter STORED_DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE_ONLY_IN = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /** "2026-07-18" -> "18-07-2026". Returns "-" for null/blank, or the raw value if it doesn't parse as a plain date. */
    public static String toIndianDateOnly(String raw) {
        if (raw == null || raw.trim().isEmpty() || "null".equalsIgnoreCase(raw.trim())) return "-";
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(raw.trim().substring(0, 10), STORED_DATE_ONLY);
            return d.format(DISPLAY_DATE_ONLY_IN);
        } catch (Exception e) {
            return raw;
        }
    }

    /** "2026-07-18 14:05:30" -> "18-07-2026 14:05:30". Returns "-" for null/blank/unparseable input. */
    public static String toIndianDateTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "-";
        try {
            LocalDateTime dt = LocalDateTime.parse(raw.trim().replace("T", " "), STORED);
            return dt.format(DISPLAY);
        } catch (DateTimeParseException e) {
            return raw; // fall back to raw value rather than hiding it
        }
    }

    /** "2026-07-18 14:05:30" -> "18-07-2026". */
    public static String toIndianDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "-";
        try {
            LocalDateTime dt = LocalDateTime.parse(raw.trim().replace("T", " "), STORED);
            return dt.format(DISPLAY_DATE_ONLY);
        } catch (DateTimeParseException e) {
            return raw.length() >= 10 ? raw.substring(0, 10) : raw;
        }
    }

    /** Human-readable elapsed time between two stored timestamps, e.g. "2d 4h 15m". Returns "-" if either is missing/unparseable. */
    public static String elapsedBetween(String startRaw, String endRaw) {
        if (startRaw == null || endRaw == null
                || startRaw.trim().isEmpty() || endRaw.trim().isEmpty()) return "-";
        try {
            LocalDateTime start = LocalDateTime.parse(startRaw.trim().replace("T", " "), STORED);
            LocalDateTime end = LocalDateTime.parse(endRaw.trim().replace("T", " "), STORED);
            long totalMinutes = java.time.Duration.between(start, end).toMinutes();
            if (totalMinutes < 0) return "-";
            long days = totalMinutes / (60 * 24);
            long hours = (totalMinutes % (60 * 24)) / 60;
            long minutes = totalMinutes % 60;
            StringBuilder sb = new StringBuilder();
            if (days > 0) sb.append(days).append("d ");
            if (hours > 0 || days > 0) sb.append(hours).append("h ");
            sb.append(minutes).append("m");
            return sb.toString().trim();
        } catch (DateTimeParseException e) {
            return "-";
        }
    }
}