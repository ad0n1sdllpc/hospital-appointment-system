package com.hospital.appointment.auth;

import com.hospital.appointment.enums.UserRole;
import com.hospital.appointment.model.*;
import com.hospital.appointment.storage.DataStore;
import com.hospital.appointment.ui.Console;
import com.hospital.appointment.util.*;

/**
 * Handles all authentication:
 *   - Login (validate credentials, return User session)
 *   - Patient self-registration
 *   - Password change
 *   - Session tracking (currentUser)
 */
public class AuthService {

    private final DataStore      store;
    private final InputValidator input;

    /** The currently logged-in user (null = not logged in). */
    private User currentUser = null;

    public AuthService(DataStore store, InputValidator input) {
        this.store = store;
        this.input = input;
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    /**
     * Prompts for credentials, validates against the user store.
     * Returns true if login succeeds (currentUser is then set).
     */
    public boolean login() {
        Console.header("LOGIN");

        while (true) {
            String username = input.readLine("  Username : ");
            if (username.equals("2")) { Console.info("Returning to main menu."); return false; }

            String password = input.readPasswordLine("  Password : ");

            User user = store.findUserByUsername(username);

            if (user == null || !user.checkPassword(password)) {
                Console.error("Invalid username or password.");
                continue;
            }

            if (!user.isActive()) {
                Console.error("This account has been deactivated. Contact the administrator.");
                return false;
            }

            currentUser = user;
            Console.success("Welcome back, " + user.getFullName() + "!  Role: " + user.getRole().getDisplayName());
            return true;
        }
    }

    // =========================================================================
    // PATIENT REGISTRATION
    // =========================================================================

    /**
     * Self-registration for new patients.
     * Creates both a User account (for login) and a Patient domain record.
     */
    public boolean register() {
        Console.header("PATIENT REGISTRATION");

        String username = "";
        String password = "";
        String confirm = "";
        String name = "";
        int age = 0;
        String address = "";
        String contact = "";
        String email = "";
        String blood = "";
        String emergency = "";

        int step = 0;
        while (step < 10) {
            if (step == 0) Console.section("Create Your Account");
            if (step == 3) Console.section("Personal Information");

            String label = switch (step) {
                case 0 -> "Registration - Username";
                case 1 -> "Registration - Password";
                case 2 -> "Registration - Confirm Password";
                case 3 -> "Registration - Full Name";
                case 4 -> "Registration - Age";
                case 5 -> "Registration - Address";
                case 6 -> "Registration - Contact Number";
                case 7 -> "Registration - Email";
                case 8 -> "Registration - Blood Type";
                case 9 -> "Registration - Emergency Contact Number";
                default -> "Registration";
            };

            int nav = input.readNavigationChoice(label);
            if (nav == 0) { Console.info("Registration cancelled."); return false; }
            if (nav == 2) {
                if (step == 0) { Console.info("Registration cancelled."); return false; }
                step--;
                continue;
            }

            switch (step) {
                case 0 -> {
                    username = input.readUsername("  Choose a Username  : ");
                    if (!store.isUsernameAvailable(username)) {
                        Console.error("Username '" + username + "' is already taken. Please try another.");
                        continue;
                    }
                }
                case 1 -> password = input.readPassword("  Choose a Password  : ");
                case 2 -> {
                    confirm = input.readPassword("  Confirm Password   : ");
                    if (!password.equals(confirm)) {
                        Console.error("Passwords do not match. Please try again.");
                        continue;
                    }
                }
                case 3 -> name = input.readFullName("  Full Name          : ");
                case 4 -> age = input.readAge("  Age               : ");
                case 5 -> address = input.readString("  Address            : ");
                case 6 -> contact = input.readContact("  Contact Number     : ");
                case 7 -> email = input.readEmail("  Email (optional)   : ");
                case 8 -> blood = input.readBloodType("  Blood Type (opt)   : ");
                case 9 -> emergency = input.readContact("  Emergency Contact Number : ");
            }
            step++;
        }

        // Create accounts
        String ts        = DateUtils.now();
        String userId    = store.ids.nextUserId();
        String patientId = store.ids.nextPatientId();

        User u = new User(userId, username, password,
            UserRole.PATIENT, patientId, name, true, ts);

        Patient p = new Patient(patientId, userId, name, age, address,
            contact, email, blood, emergency, ts);

        store.users.add(u);
        store.patients.put(patientId, p);
        store.saveUsers();
        store.savePatients();

        Console.success("Registration successful! You can now log in.");
        Console.fieldLine("  Username  ", username);
        Console.fieldLine("  Patient ID", patientId);
        return true;
    }

    // =========================================================================
    // PASSWORD CHANGE
    // =========================================================================

    /** Allows the logged-in user to change their own password. */
    public void changePassword() {
        Console.header("CHANGE PASSWORD");

        String current = input.readPasswordLine("  Current Password : ");
        if (!currentUser.checkPassword(current)) {
            Console.error("Incorrect current password.");
            return;
        }

        String newPass;
        while (true) {
            int nav = input.readNavigationChoice("Change Password - New Password");
            if (nav != 1) {
                Console.info("Password change cancelled.");
                return;
            }

            newPass = input.readPassword("  New Password      : ");
            String confirm = input.readPassword("  Confirm Password  : ");

            if (currentUser.checkPassword(newPass)) {
                Console.error("New password must be different from current password.");
                continue;
            }
            if (!newPass.equals(confirm)) {
                Console.error("Passwords do not match.");
                continue;
            }
            break;
        }

        currentUser.setPassword(newPass);
        store.saveUsers();
        Console.success("Password changed successfully.");
    }

    // =========================================================================
    // SESSION
    // =========================================================================

    public User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn()  { return currentUser != null; }

    public void logout() {
        System.out.printf("%n  Goodbye, %s. You have been logged out.%n%n",
            currentUser.getFullName());
        currentUser = null;
    }
}
