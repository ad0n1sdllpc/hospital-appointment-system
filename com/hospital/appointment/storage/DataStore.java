package com.hospital.appointment.storage;

import com.hospital.appointment.enums.*;
import com.hospital.appointment.model.*;
import com.hospital.appointment.util.IdGenerator;

import java.io.*;
import java.util.*;

/**
 * Central data store.
 *
 * Holds ALL in-memory collections and handles file I/O.
 * All dashboards read from and write to this single object — no duplication.
 *
 * Files:
 *   data/users.txt        — login accounts
 *   data/patients.txt     — patient domain records
 *   data/doctors.txt      — doctor domain records
 *   data/appointments.txt — appointment records
 *   data/waitlist.txt     — waitlist entries
 */
public class DataStore {

    // ── File paths ────────────────────────────────────────────────────────────
    private static final String DIR   = "data";
    private static final String USERS = DIR + "/users.txt";
    private static final String PATS  = DIR + "/patients.txt";
    private static final String DOCS  = DIR + "/doctors.txt";
    private static final String APTS  = DIR + "/appointments.txt";
    private static final String WL    = DIR + "/waitlist.txt";

    // ── Collections ───────────────────────────────────────────────────────────
    public final List<User>          users        = new ArrayList<>();
    public final Map<String, Patient> patients    = new LinkedHashMap<>(); // patientId -> Patient
    public final Map<String, Doctor>  doctors     = new LinkedHashMap<>(); // doctorId  -> Doctor
    public final List<Appointment>    appointments = new ArrayList<>();
    public final List<WaitlistEntry>  waitlist     = new ArrayList<>();

    public final IdGenerator ids = new IdGenerator();

    // =========================================================================
    // LOAD
    // =========================================================================

    /** Load all data from disk on startup. Calls resolve() to link FK references. */
    public void loadAll() {
        ensureDir();
        loadDoctors();
        loadPatients();
        loadUsers();
        loadAppointments();
        loadWaitlist();
        resolveAll();
        seedDefaultsIfEmpty();
    }

    // ── Individual loaders ────────────────────────────────────────────────────

    private void loadDoctors() {
        for (String line : readLines(DOCS)) {
            String[] p = split(line, 8);
            if (p == null) continue;
            try {
                String dateSlots = p[7].startsWith("DATES:") ? p[7] : Doctor.EMPTY_DATE_SLOTS;
                Doctor d = new Doctor(p[0], p[1], p[2],
                    Department.valueOf(p[3]), p[4],
                    Integer.parseInt(p[5]), p[6], dateSlots);
                doctors.put(d.getDoctorId(), d);
                ids.syncPatient(IdGenerator.seq(d.getDoctorId()));
            } catch (Exception e) { warn("doctors.txt", e); }
        }
    }

    private void loadPatients() {
        for (String line : readLines(PATS)) {
            String[] p = split(line, 10);
            if (p == null) continue;
            try {
                Patient pt = new Patient(p[0], p[1], p[2], Integer.parseInt(p[3]),
                    p[4], p[5], p[6], p[7], p[8], p[9]);
                patients.put(pt.getPatientId(), pt);
                ids.syncPatient(IdGenerator.seq(pt.getPatientId()));
            } catch (Exception e) { warn("patients.txt", e); }
        }
    }

    private void loadUsers() {
        for (String line : readLines(USERS)) {
            String[] p = split(line, 8);
            if (p == null) continue;
            try {
                User u = new User(p[0], p[1], p[2],
                    UserRole.valueOf(p[3]), p[4], p[5],
                    Boolean.parseBoolean(p[6]), p[7]);
                users.add(u);
                ids.syncUser(IdGenerator.seq(u.getUserId()));
            } catch (Exception e) { warn("users.txt", e); }
        }
    }

    private void loadAppointments() {
        for (String line : readLines(APTS)) {
            String[] p = split(line, 8);
            if (p == null) continue;
            try {
                Appointment a = new Appointment(p[0], p[1], p[2],
                    p[3], p[4], AppointmentStatus.valueOf(p[5]), p[6], p[7]);
                appointments.add(a);
                ids.syncAppointment(IdGenerator.seq(a.getAppointmentId()));
            } catch (Exception e) { warn("appointments.txt", e); }
        }
    }

    private void loadWaitlist() {
        for (String line : readLines(WL)) {
            String[] p = split(line, 8);
            if (p == null) continue;
            try {
                WaitlistEntry w = new WaitlistEntry(p[0], p[1], p[2],
                    p[3], WaitlistStatus.valueOf(p[4]), p[5],
                    Integer.parseInt(p[6]), p[7]);
                waitlist.add(w);
                ids.syncWaitlist(IdGenerator.seq(w.getWaitlistId()));
            } catch (Exception e) { warn("waitlist.txt", e); }
        }
    }

    // =========================================================================
    // SAVE
    // =========================================================================

    public void saveAll() {
        saveUsers();
        saveDoctors();
        savePatients();
        saveAppointments();
        saveWaitlist();
    }

    public void saveUsers()        { writeLines(USERS, users.stream().map(User::toFileString).toList()); }
    public void saveDoctors()      { writeLines(DOCS,  new ArrayList<>(doctors.values()).stream().map(Doctor::toFileString).toList()); }
    public void savePatients()     { writeLines(PATS,  new ArrayList<>(patients.values()).stream().map(Patient::toFileString).toList()); }
    public void saveAppointments() { writeLines(APTS,  appointments.stream().map(Appointment::toFileString).toList()); }
    public void saveWaitlist()     { writeLines(WL,    waitlist.stream().map(WaitlistEntry::toFileString).toList()); }

    // =========================================================================
    // RESOLVE — link FK references after load
    // =========================================================================

    /** Hydrates transient Patient/Doctor references on Appointment and WaitlistEntry objects. */
    public void resolveAll() {
        for (Appointment a : appointments) {
            Patient pat = patients.get(a.getPatientId());
            Doctor  doc = doctors.get(a.getDoctorId());
            a.resolve(pat, doc);
        }
        for (WaitlistEntry w : waitlist) {
            Patient pat = patients.get(w.getPatientId());
            Doctor  doc = doctors.get(w.getDoctorId());
            w.resolve(pat, doc);
        }
    }

    // =========================================================================
    // LOOKUP HELPERS
    // =========================================================================

    public User findUserByUsername(String username) {
        return users.stream()
            .filter(u -> u.getUsername().equalsIgnoreCase(username) && u.isActive())
            .findFirst().orElse(null);
    }

    public Patient findPatientByUserId(String userId) {
        return patients.values().stream()
            .filter(p -> p.getUserId().equals(userId))
            .findFirst().orElse(null);
    }

    public Doctor findDoctorByUserId(String userId) {
        return doctors.values().stream()
            .filter(d -> d.getUserId().equals(userId))
            .findFirst().orElse(null);
    }

    public Appointment findAppointmentById(String id) {
        return appointments.stream()
            .filter(a -> a.getAppointmentId().equalsIgnoreCase(id))
            .findFirst().orElse(null);
    }

    public boolean isUsernameAvailable(String username) {
        return users.stream().noneMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    // =========================================================================
    // DEFAULT SEED DATA
    // =========================================================================

    /**
     * Called only on first run (empty data directory).
     * Seeds one admin account and 12 doctors with login accounts.
     */
    private void seedDefaultsIfEmpty() {
        if (!users.isEmpty()) return;  // Data already exists — don't overwrite

        System.out.println("  [SETUP] First run detected. Seeding default accounts...");

        // Admin account
        String adminId = ids.nextUserId();
        users.add(new User(adminId, "admin", "admin123",
            UserRole.ADMIN, "", "System Administrator", true, com.hospital.appointment.util.DateUtils.now()));

        // Doctor accounts and domain records
        seedDoctor("D-01", "Santos",     com.hospital.appointment.enums.Department.CARDIOLOGY,       "Interventional Cardiology", 18, "Mon-Fri");
        seedDoctor("D-02", "Reyes",      com.hospital.appointment.enums.Department.PEDIATRICS,       "Neonatology",               12, "Mon-Sat");
        seedDoctor("D-03", "Cruz",       com.hospital.appointment.enums.Department.ORTHOPEDICS,      "Sports Medicine",           15, "Tue-Sat");
        seedDoctor("D-04", "Garcia",     com.hospital.appointment.enums.Department.NEUROLOGY,        "Stroke & Epilepsy",         20, "Mon-Fri");
        seedDoctor("D-05", "Mendoza",    com.hospital.appointment.enums.Department.DERMATOLOGY,      "Cosmetic Dermatology",       9, "Mon-Fri");
        seedDoctor("D-06", "Torres",     com.hospital.appointment.enums.Department.OPHTHALMOLOGY,    "Retina Specialist",         14, "Mon-Thu");
        seedDoctor("D-07", "Villanueva", com.hospital.appointment.enums.Department.GENERAL_MEDICINE, "Family Medicine",           11, "Mon-Sun");
        seedDoctor("D-08", "Aquino",     com.hospital.appointment.enums.Department.OBSTETRICS,       "Maternal-Fetal Medicine",   16, "Mon-Fri");
        seedDoctor("D-09", "Bautista",   com.hospital.appointment.enums.Department.ONCOLOGY,         "Medical Oncology",          22, "Mon-Fri");
        seedDoctor("D-10", "Lim",        com.hospital.appointment.enums.Department.PSYCHIATRY,       "Clinical Psychology",       10, "Mon-Fri");
        seedDoctor("D-11", "Ramos",      com.hospital.appointment.enums.Department.PULMONOLOGY,      "Critical Care",             17, "Mon-Sat");
        seedDoctor("D-12", "DelaCreuz",  com.hospital.appointment.enums.Department.ENDOCRINOLOGY,    "Diabetes & Metabolism",     13, "Mon-Fri");

        saveAll();
        System.out.println("  [SETUP] Done. Default admin: admin / admin123");
        System.out.println("  [SETUP] Doctor logins: dr.santos/doc123, dr.reyes/doc123, etc.");
        System.out.println();
    }

    private void seedDoctor(String docId, String lastName, Department dept,
                             String spec, int yrs, String sched) {
        String username = "dr." + lastName.toLowerCase().replace(" ", "");
        String userId   = ids.nextUserId();
        String ts       = com.hospital.appointment.util.DateUtils.now();

        User u = new User(userId, username, "doc123",
            UserRole.DOCTOR, docId, "Dr. " + lastName, true, ts);
        users.add(u);

        Doctor d = new Doctor(docId, userId, lastName, dept, spec, yrs, sched,
            Doctor.EMPTY_DATE_SLOTS);
        doctors.put(docId, d);
    }

    // =========================================================================
    // FILE I/O HELPERS
    // =========================================================================

    private List<String> readLines(String path) {
        List<String> lines = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) return lines;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("  [ERR] Cannot read " + path + ": " + e.getMessage());
        }
        return lines;
    }

    private void writeLines(String path, List<String> lines) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            for (String line : lines) { w.write(line); w.newLine(); }
        } catch (IOException e) {
            System.err.println("  [ERR] Cannot write " + path + ": " + e.getMessage());
        }
    }

    private String[] split(String line, int expectedFields) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < expectedFields) {
            System.err.println("  [SKIP] Malformed line (expected " + expectedFields + " fields): " + line);
            return null;
        }
        return parts;
    }

    private void ensureDir() {
        File dir = new File(DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    private void warn(String file, Exception e) {
        System.err.println("  [WARN] Parse error in " + file + ": " + e.getMessage());
    }
}
