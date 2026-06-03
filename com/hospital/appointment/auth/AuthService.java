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

        String username;
        String password;
        String name;
        int age;
        String address;
        String contact;
        String email;
        String blood;
        String emergency;

        while (true) {
            while (true) {
                Console.section("Create Your Account");
                username = input.readUsername("  Choose a Username  : ");
                if (!store.isUsernameAvailable(username)) {
                    Console.error("Username '" + username + "' is already taken. Please try another.");
                    continue;
                }

                try {
                    password = input.readPassword("  Choose a Password  : ");
                    String confirm = input.readPassword("  Confirm Password   : ");
                    if (!password.equals(confirm)) {
                        Console.error("Passwords do not match. Please try again.");
                        continue;
                    }
                    break;
                } catch (InputValidator.BackException e) {
                    Console.info("Returning to username input.");
                }
            }

            Console.section("Personal Information");
            name = input.readFullName("  Full Name          : ");
            age = input.readAge("  Age               : ");
            address = input.readString("  Address            : ");
            contact = input.readContact("  Contact Number     : ");
            email = input.readEmail("  Email (optional)   : ");
            blood = input.readBloodType("  Blood Type (opt)   : ");
            emergency = input.readContact("  Emergency Contact Number : ");

            System.out.println();
            System.out.println("  Proceed with registration?");
            System.out.println("  [1] Continue");
            System.out.println("  [2] Back");
            int confirmRegistration = input.readIntInRange("  Choice : ", 1, 2);
            if (confirmRegistration == 1) break;
            Console.info("Returning to account input.");
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
