package com.hospital.appointment.util;

import java.time.LocalDate;
import java.util.Scanner;

/**
 * Reads and validates all user input.
 * Every method keeps looping until valid input is received.
 * Centralises validation so no other class needs to handle Scanner directly.
 */
public class InputValidator {

    private static final int MAX_AGE = 130;
    private static final String FULL_NAME_REGEX = "^[A-Za-z]+(?:[ -][A-Za-z]+)*$";
    private static final String CONTACT_REGEX = "^(09\\d{2}-\\d{3}-\\d{4}|09\\d{9})$";
    private static final String EMAIL_REGEX =
        "^[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*@[A-Za-z0-9-]+(?:\\.[A-Za-z]{2,})+$";

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

    /** Full name: letters, spaces, and hyphens only */
    public String readFullName(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.matches(FULL_NAME_REGEX)) return input;
            Console.warn("Full name must use letters, spaces, and hyphens only.");
        }
    }

    /** Optional full name with the same validation */
    public String readOptionalFullName(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() || input.matches(FULL_NAME_REGEX)) return input;
            Console.warn("Full name must use letters, spaces, and hyphens only.");
        }
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

    /** Age within the system limit */
    public int readAge(String prompt) {
        return readIntInRange(prompt, 1, MAX_AGE);
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

    /** Philippine mobile number: 09XX-XXX-XXXX or 09XXXXXXXXX */
    public String readContact(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.matches(CONTACT_REGEX)) return input;
            Console.warn("Enter a valid Philippine mobile number: 09XX-XXX-XXXX or 09XXXXXXXXX.");
        }
    }

    /** Optional contact number with the same validation */
    public String readOptionalContact(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() || input.matches(CONTACT_REGEX)) return input;
            Console.warn("Enter a valid Philippine mobile number: 09XX-XXX-XXXX or 09XXXXXXXXX.");
        }
    }

    /** Email with format validation; empty is allowed */
    public String readEmail(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() || input.matches(EMAIL_REGEX)) return input;
            Console.warn("Enter a valid email address, or press Enter to skip.");
        }
    }

    /** Optional email with the same validation */
    public String readOptionalEmail(String prompt) {
        return readEmail(prompt);
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
