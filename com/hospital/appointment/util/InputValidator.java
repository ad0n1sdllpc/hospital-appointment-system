package com.hospital.appointment.util;

import java.time.LocalDate;
import java.util.Scanner;

/**
 * Reads and validates all user input.
 * Every method keeps looping until valid input is received.
 * Centralises validation so no other class needs to handle Scanner directly.
 */
public class InputValidator {

    private final Scanner scanner;

    public InputValidator(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Non-blank string */
    public String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String v = scanner.nextLine().trim();
            if (!v.isEmpty()) return v;
            Console.warn("Input cannot be blank.");
        }
    }

    /** Optional string — returns "" if user presses Enter */
    public String readOptional(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Positive integer (e.g., age) */
    public int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(scanner.nextLine().trim());
                if (v > 0) return v;
                Console.warn("Value must be greater than zero.");
            } catch (NumberFormatException e) {
                Console.warn("Please enter a valid whole number.");
            }
        }
    }

    /** Integer within [min, max] inclusive */
    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(scanner.nextLine().trim());
                if (v >= min && v <= max) return v;
                Console.warn("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                Console.warn("Invalid input — please enter a number.");
            }
        }
    }

    /** Future (or today) date in yyyy-MM-dd */
    public String readFutureDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            LocalDate date = DateUtils.parseDate(input);
            if (date == null)                         { Console.warn("Use yyyy-MM-dd format, e.g. 2026-06-20."); continue; }
            if (date.isBefore(LocalDate.now()))       { Console.warn("Cannot select a past date."); continue; }
            return input;
        }
    }

    /** Any valid date in yyyy-MM-dd (no future restriction) */
    public String readAnyDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (DateUtils.parseDate(input) != null) return input;
            Console.warn("Use yyyy-MM-dd format, e.g. 2026-06-20.");
        }
    }

    /** Contact number: 7-15 digits */
    public String readContact(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            String digits = input.replaceAll("[^0-9]", "");
            if (digits.length() >= 7 && digits.length() <= 15) return input;
            Console.warn("Enter a valid contact number (7-15 digits).");
        }
    }

    /** Email with basic validation; empty is allowed */
    public String readEmail(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() || (input.contains("@") && input.contains("."))) return input;
            Console.warn("Enter a valid email address, or press Enter to skip.");
        }
    }

    /** Blood type or empty */
    public String readBloodType(String prompt) {
        String[] valid = {"A+","A-","B+","B-","AB+","AB-","O+","O-",""};
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toUpperCase();
            for (String v : valid) { if (v.equals(input)) return input; }
            Console.warn("Valid: A+, A-, B+, B-, AB+, AB-, O+, O- (or Enter to skip).");
        }
    }

    /** Username: letters/digits/underscore, 4-20 chars */
    public String readUsername(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.matches("[a-zA-Z0-9_]{4,20}")) return input;
            Console.warn("Username: 4-20 chars, letters/digits/underscore only.");
        }
    }

    /** Password: minimum 6 chars */
    public String readPassword(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.length() >= 6) return input;
            Console.warn("Password must be at least 6 characters.");
        }
    }

    /** Yes/no — returns true for yes */
    public boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) return true;
            if (input.equals("n") || input.equals("no"))  return false;
            Console.warn("Please enter y or n.");
        }
    }

    // Shortcut for Console (avoids circular import if Console extends this)
    private static class Console {
        static void warn(String msg) { System.out.println("  [!] " + msg); }
    }
}
