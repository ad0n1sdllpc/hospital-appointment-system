package com.hospital.appointment;

import com.hospital.appointment.auth.AuthService;
import com.hospital.appointment.dashboard.*;
import com.hospital.appointment.model.User;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.appointment.storage.DataStore;
import com.hospital.appointment.ui.Console;
import com.hospital.appointment.util.InputValidator;

import java.util.Scanner;

/**
 * ============================================================
 *  Hospital Management System  — Main Entry Point
 * ============================================================
 *
 *  Responsibilities (ONLY):
 *    1. Bootstrap: load data, wire dependencies
 *    2. Show the guest menu (Login / Register / Exit)
 *    3. After login, route to the correct role dashboard
 *    4. Repeat until the user exits
 *
 *  Zero business logic here.
 *  All logic is in AuthService, AppointmentService, and Dashboards.
 */
public class Main {

    public static void main(String[] args) {

        // ── Bootstrap ─────────────────────────────────────────────────────────
        Scanner scanner = new Scanner(System.in);

        DataStore store = new DataStore();
        store.loadAll();                               // Load all 5 data files

        InputValidator    input   = new InputValidator(scanner);
        AppointmentService apptSvc = new AppointmentService(store, input);
        AuthService        auth    = new AuthService(store, input);

        // ── Welcome ───────────────────────────────────────────────────────────
        Console.welcomeBanner();

        // ── Main application loop ─────────────────────────────────────────────
        boolean appRunning = true;

        while (appRunning) {

            try {
                // Guest menu: shown when no one is logged in
                Console.guestMenu();
                int choice = input.readIntInRange("  Your choice : ", 0, 2);
                System.out.println();

                switch (choice) {

                // ── [1] Login ─────────────────────────────────────────────────
                case 1 -> {
                    boolean loggedIn = auth.login();
                    if (loggedIn) {
                        routeToDashboard(auth, store, apptSvc, input, scanner);
                        // After logout, loop back to the guest menu
                    }
                }

                // ── [2] Register as Patient ───────────────────────────────────
                case 2 -> {
                    boolean registered = auth.register();
                    if (registered) {
                        // Auto-login after successful registration
                        Console.info("You can now log in with your new credentials.");
                    }
                    Console.pause(scanner);
                }

                // ── [0] Exit ──────────────────────────────────────────────────
                case 0 -> {
                    Console.goodbye();
                    appRunning = false;
                }

                default -> Console.warn("Invalid choice. Please select 0, 1, or 2.");
                }
            } catch (InputValidator.ExitException | InputValidator.BackException e) {
                Console.info("Returning to main menu.");
            }
        }

        scanner.close();
    }

    // =========================================================================
    // ROLE ROUTER
    // =========================================================================

    /**
     * Called right after a successful login.
     * Reads the user's role and hands control to the matching dashboard.
     * Returns when the user logs out (the dashboard loop exits).
     */
    private static void routeToDashboard(AuthService auth, DataStore store,
                                          AppointmentService apptSvc,
                                          InputValidator input, Scanner scanner) {
        User user = auth.getCurrentUser();

        boolean dashboardRunning = true;
        while (dashboardRunning) {
            try {
                switch (user.getRole()) {

                    case ADMIN -> {
                        AdminDashboard admin = new AdminDashboard(store, apptSvc, input, user);
                        admin.run(scanner);
                    }

                    case DOCTOR -> {
                        DoctorDashboard doctor = new DoctorDashboard(store, apptSvc, input, user);
                        doctor.run(scanner);
                    }

                    case PATIENT -> {
                        PatientDashboard patient = new PatientDashboard(store, apptSvc, input, auth, user);
                        patient.run(scanner);
                    }

                    default -> Console.error("Unknown role: " + user.getRole() + ". Contact system admin.");
                }
                dashboardRunning = false;
            } catch (InputValidator.ExitException | InputValidator.BackException e) {
                Console.info("Returning to dashboard.");
            }
        }

        auth.logout();
    }
}
