package com.hospital.appointment.util;

public final class InputValidator {
    private InputValidator() {
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }

    public static int parsePositiveAge(String value) {
        requireNonBlank(value, "Age");

        try {
            int age = Integer.parseInt(value.trim());
            if (age <= 0) {
                throw new IllegalArgumentException("Age must be a positive number.");
            }
            return age;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Age must be a valid number.");
        }
    }

    public static int parseMenuChoice(String value, int min, int max) {
        requireNonBlank(value, "Menu choice");

        try {
            int choice = Integer.parseInt(value.trim());
            if (choice < min || choice > max) {
                throw new IllegalArgumentException("Choice must be between " + min + " and " + max + ".");
            }
            return choice;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid numeric menu choice.");
        }
    }

    public static String safeLower(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }
}