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

        String username = input.readString("  Username : ");
        String password = input.readString("  Password : ");

        User user = store.findUserByUsername(username);

        if (user == null || !user.checkPassword(password)) {
            Console.error("Invalid username or password. Please try again.");
            return false;
        }

        if (!user.isActive()) {
            Console.error("This account has been deactivated. Contact the administrator.");
            return false;
        }

        currentUser = user;
        Console.success("Welcome back, " + user.getFullName() + "!  Role: " + user.getRole().getDisplayName());
        return true;
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

        Console.section("Create Your Account");
        String username = input.readUsername ("  Choose a Username  : ");
        if (!store.isUsernameAvailable(username)) {
            Console.error("Username '" + username + "' is already taken. Please try another.");
            return false;
        }
        String password = input.readPassword ("  Choose a Password  : ");
        String confirm  = input.readPassword ("  Confirm Password   : ");
        if (!password.equals(confirm)) {
            Console.error("Passwords do not match. Registration cancelled.");
            return false;
        }

        Console.section("Personal Information");
        String name      = input.readFullName ("  Full Name          : ");
        int    age       = input.readAge      ("  Age               : ");
        String address   = input.readString   ("  Address            : ");
        String contact   = input.readContact  ("  Contact Number     : ");
        String email     = input.readEmail    ("  Email (optional)   : ");
        String blood     = input.readBloodType("  Blood Type (opt)   : ");
        String emergency = input.readContact  ("  Emergency Contact Number : ");

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

        String current = input.readString  ("  Current Password : ");
        if (!currentUser.checkPassword(current)) {
            Console.error("Incorrect current password.");
            return;
        }
        String newPass  = input.readPassword("  New Password      : ");
        String confirm  = input.readPassword("  Confirm Password  : ");
        if (!newPass.equals(confirm)) {
            Console.error("Passwords do not match.");
            return;
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
