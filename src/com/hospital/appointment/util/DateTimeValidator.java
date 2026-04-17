package com.hospital.appointment.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimeValidator {
    private static final DateTimeFormatter INPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter OUTPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeValidator() {
    }

    public static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Date format must be yyyy-MM-dd.");
        }
    }

    public static LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value.trim(), INPUT_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Time format must be HH:mm.");
        }
    }

    public static void ensureNotPast(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot book an appointment in the past.");
        }
    }

    public static String formatTime(LocalTime time) {
        return time.format(OUTPUT_TIME_FORMATTER);
    }
}