package com.hospital.appointment.dashboard;

import com.hospital.appointment.auth.AuthService;
import com.hospital.appointment.model.*;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.appointment.storage.DataStore;
import com.hospital.appointment.ui.Console;
import com.hospital.appointment.util.*;

/**
 * Patient Dashboard — scoped to the logged-in patient's own records.
 *
 * Options:
 *   [1] Book New Appointment (includes Waitlist when full)
 *   [2] My Upcoming Appointments
 *   [3] Cancel My Appointment
 *   [4] My Appointment History
 *   [5] Update My Profile
 *   [6] Change Password
 */
public class PatientDashboard {

    private final DataStore         store;
    private final AppointmentService apptSvc;
    private final InputValidator    input;
    private final AuthService       auth;
    private final User              user;
    private       Patient           patient; // The domain record for this patient

    public PatientDashboard(DataStore store, AppointmentService apptSvc,
                             InputValidator input, AuthService auth, User user) {
        this.store   = store;
        this.apptSvc = apptSvc;
        this.input   = input;
        this.auth    = auth;
        this.user    = user;
        this.patient = store.findPatientByUserId(user.getUserId());
    }

    /** Main loop. Returns when patient logs out. */
    public void run(java.util.Scanner scanner) {
        if (patient == null) {
            Console.error("No patient profile found for this account. Contact admin.");
            return;
        }

        boolean running = true;
        while (running) {
            Console.patientDashboard(user.getFullName());
            int choice = input.readIntInRange("  Your choice : ", 0, 6);
            System.out.println();

            switch (choice) {
                case 1 -> apptSvc.bookAppointment(patient);
                case 2 -> apptSvc.viewPatientAppointments(patient.getPatientId(), true);
                case 3 -> apptSvc.cancelAppointment(patient.getPatientId());
                case 4 -> apptSvc.viewPatientAppointments(patient.getPatientId(), false);
                case 5 -> updateProfile();
                case 6 -> auth.changePassword();
                case 0 -> running = false;
                default -> Console.warn("Invalid choice.");
            }
            if (running) Console.pause(scanner);
        }
    }

    // =========================================================================
    // UPDATE PROFILE
    // =========================================================================

    private void updateProfile() {
        Console.header("UPDATE MY PROFILE");

        Console.fieldLine("  Name",    patient.getName());
        Console.fieldLine("  Age",     String.valueOf(patient.getAge()));
        Console.fieldLine("  Address", patient.getAddress());
        Console.fieldLine("  Contact", patient.getContactNumber());
        Console.fieldLine("  Email",   patient.getEmail());
        Console.fieldLine("  Blood",   patient.getBloodType().isEmpty() ? "N/A" : patient.getBloodType());
        System.out.println();
        System.out.println("  Press Enter to keep any field unchanged.");
        System.out.println();

        String name    = input.readOptional("  Full Name          : ");
        String address = input.readOptional("  Address            : ");
        String contact = input.readOptional("  Contact Number     : ");
        String email   = input.readOptional("  Email              : ");

        if (!name.isEmpty())    patient.setName(name);
        if (!address.isEmpty()) patient.setAddress(address);
        if (!contact.isEmpty()) patient.setContactNumber(contact);
        if (!email.isEmpty())   patient.setEmail(email);

        // Also update full name on user account
        if (!name.isEmpty()) {
            user.setFullName(name);
            store.saveUsers();
        }

        store.savePatients();
        Console.success("Profile updated successfully.");
    }
}
