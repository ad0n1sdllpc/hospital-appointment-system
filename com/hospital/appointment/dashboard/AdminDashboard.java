package com.hospital.appointment.dashboard;

import com.hospital.appointment.enums.Department;
import com.hospital.appointment.enums.UserRole;
import com.hospital.appointment.model.*;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.appointment.storage.DataStore;
import com.hospital.appointment.ui.Console;
import com.hospital.appointment.util.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin Dashboard — full system access.
 *
 * Options:
 *   Appointments : book, view all, detail, reschedule, cancel, complete, schedule
 *   Management   : manage doctors, manage patients, waitlist, report
 *   Search/Filter: search all, filter by status, filter by department
 */
public class AdminDashboard {

    private final DataStore         store;
    private final AppointmentService apptSvc;
    private final InputValidator    input;
    private final User              admin;

    public AdminDashboard(DataStore store, AppointmentService apptSvc,
                           InputValidator input, User admin) {
        this.store   = store;
        this.apptSvc = apptSvc;
        this.input   = input;
        this.admin   = admin;
    }

    /** Main loop for the Admin dashboard. Returns when admin logs out. */
    public void run(java.util.Scanner scanner) {
        boolean running = true;
        while (running) {
            Console.adminDashboard(admin.getFullName());
            int choice = input.readIntInRange("  Your choice : ", 0, 15);
            System.out.println();

            switch (choice) {
                // ── Appointments ─────────────────────────────────────────────
                case 1  -> apptSvc.bookAppointment(null);
                case 2  -> apptSvc.viewAllAppointments();
                case 3  -> apptSvc.viewDetail();
                case 4  -> apptSvc.rescheduleAppointment();
                case 5  -> apptSvc.cancelAppointment(null);
                case 6  -> apptSvc.completeAppointment(null);
                case 7  -> apptSvc.viewDoctorSchedule();
                case 8  -> manageWaitlist();
                // ── Management ───────────────────────────────────────────────
                case 9  -> manageDoctors();
                case 10 -> managePatients();
                case 11 -> apptSvc.viewWaitlist();
                case 12 -> apptSvc.viewReport();
                // ── Search & Filter ──────────────────────────────────────────
                case 13 -> apptSvc.searchAll();
                case 14 -> apptSvc.filterByStatus();
                case 15 -> apptSvc.filterByDepartment();
                // ── Logout ───────────────────────────────────────────────────
                case 0  -> running = false;
                default -> Console.warn("Invalid choice.");
            }
            if (running) Console.pause(scanner);
        }
    }

    // =========================================================================
    // MANAGE DOCTORS
    // =========================================================================

    private void manageDoctors() {
        Console.header("MANAGE DOCTORS");
        System.out.println("  [1] View All Doctors");
        System.out.println("  [2] Add New Doctor");
        System.out.println("  [3] Edit Doctor Info");
        System.out.println("  [4] Deactivate Doctor Account");
        System.out.println("  [0] Back");

        int choice = input.readIntInRange("\n  Choice : ", 0, 4);
        switch (choice) {
            case 1 -> listAllDoctors();
            case 2 -> addDoctor();
            case 3 -> editDoctor();
            case 4 -> deactivateDoctorAccount();
            case 0 -> {}
        }
    }

    private void listAllDoctors() {
        Console.header("ALL DOCTORS");
        if (store.doctors.isEmpty()) { Console.info("No doctors registered."); return; }
        System.out.printf("  %-6s %-22s %-28s %-14s %-5s%n",
            "ID", "Name", "Department", "Schedule", "Exp");
        System.out.println("  " + "-".repeat(80));
        store.doctors.values().forEach(d ->
            System.out.printf("  %-6s %-22s %-28s %-14s %d yrs%n",
                d.getDoctorId(), d.getName(),
                d.getDepartment().getDisplayName(),
                d.getSchedule(), d.getYearsOfExperience()));
    }

    private void addDoctor() {
        Console.header("ADD NEW DOCTOR");

        Console.section("Doctor Information");
        String lastName = input.readString   ("  Last Name           : ");
        String spec     = input.readString   ("  Specialization      : ");
        int    yrs      = input.readPositiveInt("  Years of Experience: ");
        String sched    = input.readString   ("  Schedule (e.g. Mon-Fri) : ");

        Console.section("Select Department");
        Department[] depts = Department.values();
        for (int i = 0; i < depts.length; i++)
            System.out.printf("  [%2d] %s%n", i + 1, depts[i].getDisplayName());
        int dChoice = input.readIntInRange("\n  Department : ", 1, depts.length);
        Department dept = depts[dChoice - 1];

        Console.section("Create Login Account");
        String username = input.readUsername("  Username   : ");
        if (!store.isUsernameAvailable(username)) {
            Console.error("Username already taken.");
            return;
        }
        String password = input.readPassword("  Password   : ");

        String ts     = DateUtils.now();
        String userId = store.ids.nextUserId();
        String docId  = nextDoctorId();

        User u = new User(userId, username, password,
            UserRole.DOCTOR, docId, "Dr. " + lastName, true, ts);
        Doctor d = new Doctor(docId, userId, lastName, dept, spec, yrs, sched,
            Doctor.EMPTY_DATE_SLOTS);

        store.users.add(u);
        store.doctors.put(docId, d);
        store.saveUsers();
        store.saveDoctors();

        Console.success("Doctor added successfully!");
        Console.fieldLine("  Doctor ID", docId);
        Console.fieldLine("  Username ", username);
    }

    private void editDoctor() {
        Console.header("EDIT DOCTOR");
        String id = input.readString("  Doctor ID : ").toUpperCase();
        Doctor d  = store.doctors.get(id);
        if (d == null) { Console.error("Doctor not found: " + id); return; }

        System.out.printf("  Current schedule     : %s%n", d.getSchedule());
        System.out.printf("  Current specialization: %s%n", d.getSpecialization());
        System.out.println();

        String newSched = input.readOptional("  New schedule (Enter to keep)       : ");
        String newSpec  = input.readOptional("  New specialization (Enter to keep) : ");

        if (!newSched.isEmpty()) d.setSchedule(newSched);
        if (!newSpec.isEmpty())  d.setSpecialization(newSpec);

        store.saveDoctors();
        Console.success("Doctor record updated.");
    }

    private void deactivateDoctorAccount() {
        Console.header("DEACTIVATE DOCTOR ACCOUNT");
        String docId = input.readString("  Doctor ID : ").toUpperCase();
        Doctor d = store.doctors.get(docId);
        if (d == null) { Console.error("Doctor not found."); return; }

        User u = store.users.stream()
            .filter(usr -> usr.getUserId().equals(d.getUserId()))
            .findFirst().orElse(null);
        if (u == null) { Console.error("No account linked to this doctor."); return; }
        if (!input.readYesNo("  Deactivate Dr. " + d.getName() + "? (y/n) : ")) return;

        u.setActive(false);
        store.saveUsers();
        Console.success("Doctor account deactivated.");
    }

    // =========================================================================
    // MANAGE PATIENTS
    // =========================================================================

    private void managePatients() {
        Console.header("MANAGE PATIENTS");
        System.out.println("  [1] View All Patients");
        System.out.println("  [2] Search Patient");
        System.out.println("  [3] View Patient Appointments");
        System.out.println("  [4] Deactivate Patient Account");
        System.out.println("  [0] Back");

        int choice = input.readIntInRange("\n  Choice : ", 0, 4);
        switch (choice) {
            case 1 -> listAllPatients();
            case 2 -> searchPatient();
            case 3 -> viewPatientAppts();
            case 4 -> deactivatePatientAccount();
            case 0 -> {}
        }
    }

    private void listAllPatients() {
        Console.header("ALL PATIENTS");
        if (store.patients.isEmpty()) { Console.info("No patients registered."); return; }
        System.out.printf("  %-14s %-24s %-5s %-8s %s%n",
            "Patient ID", "Name", "Age", "Blood", "Contact");
        System.out.println("  " + "-".repeat(70));
        store.patients.values().forEach(p ->
            System.out.printf("  %-14s %-24s %-5d %-8s %s%n",
                p.getPatientId(), p.getName(), p.getAge(),
                p.getBloodType().isEmpty() ? "N/A" : p.getBloodType(),
                p.getContactNumber()));
    }

    private void searchPatient() {
        Console.header("SEARCH PATIENT");
        String kw = input.readString("  Name or ID keyword : ").toLowerCase();
        List<Patient> found = new ArrayList<>(store.patients.values());
        found.removeIf(p -> !p.getName().toLowerCase().contains(kw)
                         && !p.getPatientId().toLowerCase().contains(kw));
        if (found.isEmpty()) { Console.info("No matches."); return; }
        found.forEach(p -> {
            System.out.println("  " + "-".repeat(50));
            Console.fieldLine("  Patient ID", p.getPatientId());
            Console.fieldLine("  Name",       p.getName());
            Console.fieldLine("  Age",        String.valueOf(p.getAge()));
            Console.fieldLine("  Contact",    p.getContactNumber());
            Console.fieldLine("  Blood Type", p.getBloodType().isEmpty() ? "N/A" : p.getBloodType());
        });
    }

    private void viewPatientAppts() {
        String pid = input.readString("  Patient ID : ").toUpperCase();
        Patient p  = store.patients.get(pid);
        if (p == null) { Console.error("Patient not found."); return; }
        apptSvc.viewPatientAppointments(pid, false);
    }

    private void deactivatePatientAccount() {
        Console.header("DEACTIVATE PATIENT ACCOUNT");
        String pid = input.readString("  Patient ID : ").toUpperCase();
        Patient p  = store.patients.get(pid);
        if (p == null) { Console.error("Patient not found."); return; }

        User u = store.users.stream()
            .filter(usr -> usr.getLinkedId().equals(pid))
            .findFirst().orElse(null);
        if (u == null) { Console.warn("No login account linked to this patient."); return; }
        if (!input.readYesNo("  Deactivate " + p.getName() + "'s account? (y/n) : ")) return;

        u.setActive(false);
        store.saveUsers();
        Console.success("Patient account deactivated.");
    }

    // =========================================================================
    // MANAGE WAITLIST
    // =========================================================================

    private void manageWaitlist() {
        Console.header("MANAGE WAITLIST");
        System.out.println("  [1] View Waitlist");
        System.out.println("  [2] Remove Entry");
        System.out.println("  [0] Back");
        int choice = input.readIntInRange("\n  Choice : ", 0, 2);
        if (choice == 1) apptSvc.viewWaitlist();
        if (choice == 2) apptSvc.removeFromWaitlist();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String nextDoctorId() {
        int max = store.doctors.keySet().stream()
            .mapToInt(k -> IdGenerator.seq(k))
            .max().orElse(0);
        return String.format("D-%02d", max + 1);
    }
}
