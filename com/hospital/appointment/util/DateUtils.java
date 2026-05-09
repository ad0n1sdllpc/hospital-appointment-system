package com.hospital.appointment.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** All date/time parsing, formatting, and timestamp helpers. */
public class DateUtils {

    private static final DateTimeFormatter ISO   = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter PRETTY= DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Parse yyyy-MM-dd string; returns null on failure. */
    public static LocalDate parseDate(String input) {
        if (input == null) return null;
        try { return LocalDate.parse(input.trim(), ISO); }
        catch (DateTimeParseException e) { return null; }
    }

    /** "May 20, 2026" */
    public static String pretty(String isoDate) {
        LocalDate d = parseDate(isoDate);
        return d != null ? d.format(PRETTY) : isoDate;
    }

    /** Current timestamp: "2026-05-10 09:30:00" */
    public static String now() {
        return LocalDateTime.now().format(STAMP);
    }

    /** Today as yyyy-MM-dd */
    public static String today() {
        return LocalDate.now().format(ISO);
    }
}
