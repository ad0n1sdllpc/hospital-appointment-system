package com.hospital.appointment.service;

import com.hospital.appointment.enums.*;
import com.hospital.appointment.model.*;
import com.hospital.appointment.storage.DataStore;
import com.hospital.appointment.ui.Console;
import com.hospital.appointment.util.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * Shared appointment business logic used by all three dashboards.
 *
 * This is NOT a dashboard itself — it's a reusable service layer.
 * Dashboards call these methods and pass context (e.g., which patient).
 */
public class AppointmentService {

    // ── Time slots available in a day ────────────────────────────────────────
    public static final List<String> DEFAULT_SLOTS = Arrays.asList(
        "00:00","01:00","02:00","03:00","04:00","05:00","06:00","07:00",
        "08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00",
        "16:00","17:00","18:00","19:00","20:00","21:00","22:00","23:00"
    );

    private static final String DATE_SLOTS_PREFIX = "DATES:";
    private static final String UNAVAILABLE_PREFIX = "UNAVAILABLE:";
    private static final String SCHEDULE_SECTION_SEPARATOR = "#";
    private static final String DATE_RANGE_SEPARATOR = "..";
    private static final String NO_SLOTS = "NONE";
    private static final boolean SLOT_DEBUG = true;

    private static final ConcernOption[] CONCERN_OPTIONS = {
        new ConcernOption("Chest pain, palpitations, high blood pressure", Department.CARDIOLOGY),
        new ConcernOption("Difficulty breathing, cough, asthma, lung concern", Department.PULMONOLOGY),
        new ConcernOption("Skin rashes, acne, itching, mole or skin concern", Department.DERMATOLOGY),
        new ConcernOption("Bone, joint, back pain, sprain, injury", Department.ORTHOPEDICS),
        new ConcernOption("Headache, seizure, numbness, dizziness, nerve concern", Department.NEUROLOGY),
        new ConcernOption("Eye pain, blurry vision, redness, vision concern", Department.OPHTHALMOLOGY),
        new ConcernOption("Child fever, child check-up, pediatric concern", Department.PEDIATRICS),
        new ConcernOption("Pregnancy, menstrual concern, reproductive health", Department.OBSTETRICS),
        new ConcernOption("Diabetes, thyroid, hormones, metabolism concern", Department.ENDOCRINOLOGY),
        new ConcernOption("Anxiety, depression, stress, sleep or mental health", Department.PSYCHIATRY),
        new ConcernOption("Cancer screening, tumor, chemotherapy concern", Department.ONCOLOGY)
    };

    private final DataStore      store;
    private final InputValidator input;

    public AppointmentService(DataStore store, InputValidator input) {
        this.store = store;
        this.input = input;
    }

    // =========================================================================
    // BOOKING
    // =========================================================================

    /**
     * Full booking flow — used by both Admin and Patient dashboards.
     *
     * @param preselectedPatient  null = admin mode (admin picks patient);
     *                            non-null = patient mode (patient books for themselves)
     */
    public void bookAppointment(Patient preselectedPatient) {
        if (System.currentTimeMillis() >= 0) {
            bookAppointmentNavigable(preselectedPatient);
            return;
        }

        Console.header("BOOK NEW APPOINTMENT");

        if (preselectedPatient == null) {
            int nav = input.readNavigationChoice("Booking - Patient");
            if (nav != 1) { Console.info("Booking cancelled."); return; }
        }

        // ── 1. Resolve patient ────────────────────────────────────────────────
        Patient patient;
        if (preselectedPatient != null) {
            patient = preselectedPatient;
            Console.fieldLine("  Booking for", patient.getName() + " [" + patient.getPatientId() + "]");
        } else {
            // Admin mode: pick or create patient
            patient = adminPickOrCreatePatient();
            if (patient == null) return;
        }

        // ── 2. Select doctor ──────────────────────────────────────────────────
        int doctorNav = input.readNavigationChoice("Booking - Doctor");
        if (doctorNav != 1) { Console.info("Booking cancelled."); return; }
        Doctor doctor = preselectedPatient != null
            ? promptGuidedDoctorSelect()
            : promptDoctorSelect();
        if (doctor == null) return;

        // ── 3. Select date ────────────────────────────────────────────────────
        int dateNav = input.readNavigationChoice("Booking - Date");
        if (dateNav != 1) { Console.info("Booking cancelled."); return; }
        Console.section("Date & Time");
        String date = promptDoctorAvailableDate(doctor, "  Preferred Date (yyyy-MM-dd) : ");

        // ── 4. Select time slot ───────────────────────────────────────────────
        int slotNav = input.readNavigationChoice("Booking - Time Slot");
        if (slotNav != 1) { Console.info("Booking cancelled."); return; }
        String slot = promptSlotSelect(doctor, date);
        if (slot == null) {
            Console.warn("No available slots. Booking cancelled.");
            return;
        }

        // ── 5. Notes ─────────────────────────────────────────────────────────
        int notesNav = input.readNavigationChoice("Booking - Notes");
        if (notesNav != 1) { Console.info("Booking cancelled."); return; }
        String notes = input.readOptional("  Reason / Notes (optional)  : ");

        // ── 6. Confirm ────────────────────────────────────────────────────────
        Console.confirmBox(patient.getName(), doctor.getName(),
            doctor.getDepartment().getDisplayName(), date, slot);

        int confirmNav = input.readNavigationChoice("Booking - Confirm");
        if (confirmNav != 1) {
            Console.info("Booking cancelled.");
            return;
        }

        // ── 7. Persist ────────────────────────────────────────────────────────
        debugSlotStates("Before booking - selected slot reserved", doctor, date, slot);

        String ts   = DateUtils.now();
        String id   = store.ids.nextAppointmentId();

        Appointment appt = new Appointment(id, patient.getPatientId(),
            doctor.getDoctorId(), date, slot, AppointmentStatus.BOOKED, notes, ts);
        appt.resolve(patient, doctor);

        store.appointments.add(appt);
        store.saveAppointments();
        debugSlotStates("After booking - selected slot booked", doctor, date, slot);

        Console.success("Appointment booked successfully!");
        Console.fieldLine("  Appointment ID", id);
        Console.fieldLine("  Patient       ", patient.getName());
        Console.fieldLine("  Doctor        ", "Dr. " + doctor.getName());
        Console.fieldLine("  Date & Time   ", DateUtils.pretty(date) + " at " + slot);
    }

    private void bookAppointmentNavigable(Patient preselectedPatient) {
        Console.header("BOOK NEW APPOINTMENT");

        Patient patient = preselectedPatient;
        Doctor doctor = null;
        String date = "";
        String slot = "";
        String notes = "";
        int firstStep = preselectedPatient == null ? 0 : 1;
        int step = firstStep;

        if (patient != null) {
            Console.fieldLine("  Booking for", patient.getName() + " [" + patient.getPatientId() + "]");
        }

        while (step <= 5) {
            String label = switch (step) {
                case 0 -> "Booking - Patient";
                case 1 -> "Booking - Doctor";
                case 2 -> "Booking - Date";
                case 3 -> "Booking - Time Slot";
                case 4 -> "Booking - Notes";
                case 5 -> "Booking - Confirm";
                default -> "Booking";
            };

            int nav = input.readNavigationChoice(label);
            if (nav == 0) { Console.info("Booking cancelled."); return; }
            if (nav == 2) {
                if (step == firstStep) { Console.info("Booking cancelled."); return; }
                step--;
                continue;
            }

            switch (step) {
                case 0 -> {
                    patient = adminPickOrCreatePatient();
                    if (patient == null) return;
                }
                case 1 -> {
                    doctor = preselectedPatient != null ? promptGuidedDoctorSelect() : promptDoctorSelect();
                    if (doctor == null) return;
                    date = "";
                    slot = "";
                }
                case 2 -> {
                    Console.section("Date & Time");
                    date = preselectedPatient != null
                        ? input.readFutureDate("  Preferred Date (yyyy-MM-dd) : ")
                        : input.readFutureDate("  Preferred Date (yyyy-MM-dd) : ");
                    slot = "";
                }
                case 3 -> {
                    slot = preselectedPatient == null
                        ? promptAdminSlotSelect(doctor, date)
                        : promptSlotSelect(doctor, date);
                    if (slot == null) {
                        if (preselectedPatient != null) {
                            BookingSelection fallback = promptPatientAvailabilityFallback(doctor, date);
                            if (fallback == null) return;
                            doctor = fallback.doctor;
                            date = fallback.date;
                            slot = fallback.slot;
                        } else {
                            Console.warn("No available slots. Booking cancelled.");
                            return;
                        }
                    }
                }
                case 4 -> notes = input.readOptional("  Reason / Notes (optional)  : ");
                case 5 -> {
                    Console.confirmBox(patient.getName(), doctor.getName(),
                        doctor.getDepartment().getDisplayName(), date, slot);
                    Console.info("Choose Continue to confirm this booking.");
                }
            }
            step++;
        }

        debugSlotStates("Before booking - selected slot reserved", doctor, date, slot);

        String ts   = DateUtils.now();
        String id   = store.ids.nextAppointmentId();

        Appointment appt = new Appointment(id, patient.getPatientId(),
            doctor.getDoctorId(), date, slot, AppointmentStatus.BOOKED, notes, ts);
        appt.resolve(patient, doctor);

        store.appointments.add(appt);
        store.saveAppointments();
        debugSlotStates("After booking - selected slot booked", doctor, date, slot);

        Console.success("Appointment booked successfully!");
        Console.fieldLine("  Appointment ID", id);
        Console.fieldLine("  Patient       ", patient.getName());
        Console.fieldLine("  Doctor        ", "Dr. " + doctor.getName());
        Console.fieldLine("  Date & Time   ", DateUtils.pretty(date) + " at " + slot);
    }

    // =========================================================================
    // VIEW LISTS
    // =========================================================================

    /** Display all appointments (admin view). */
    public void viewAllAppointments() {
        Console.header("ALL APPOINTMENTS");
        List<Appointment> list = store.appointments;
        if (list.isEmpty()) { Console.info("No appointments on record."); return; }
        printTable(list, true);
    }

    /** Display appointments for a specific patient. */
    public void viewPatientAppointments(String patientId, boolean upcomingOnly) {
        String title = upcomingOnly ? "MY UPCOMING APPOINTMENTS" : "MY APPOINTMENT HISTORY";
        Console.header(title);

        List<Appointment> list = store.appointments.stream()
            .filter(a -> a.getPatientId().equals(patientId))
            .filter(a -> !upcomingOnly || a.getStatus() == AppointmentStatus.BOOKED)
            .collect(Collectors.toList());

        if (list.isEmpty()) { Console.info("No appointments found."); return; }
        printTable(list, true);
    }

    /** Display appointments for a specific doctor, optionally filtered to today. */
    public void viewDoctorAppointments(String doctorId, boolean todayOnly) {
        String title = todayOnly ? "TODAY'S APPOINTMENTS" : "MY FULL SCHEDULE";
        Console.header(title);

        String today = DateUtils.today();
        List<Appointment> list = store.appointments.stream()
            .filter(a -> a.getDoctorId().equals(doctorId))
            .filter(a -> !todayOnly || a.getDate().equals(today))
            .filter(a -> a.getStatus() == AppointmentStatus.BOOKED
                      || a.getStatus() == AppointmentStatus.COMPLETED)
            .sorted(Comparator.comparing(Appointment::getDate)
                .thenComparing(Appointment::getTimeSlot))
            .collect(Collectors.toList());

        if (list.isEmpty()) { Console.info("No appointments found."); return; }
        printTable(list, true);
    }

    // =========================================================================
    // DETAIL VIEW
    // =========================================================================

    public void viewDetail() {
        Console.header("APPOINTMENT DETAIL");
        String id = input.readString("  Appointment ID : ").toUpperCase();
        Appointment a = store.findAppointmentById(id);
        if (a == null) { Console.error("Not found: " + id); return; }
        System.out.println(a.toDetailBlock());
    }

    /** Detail view restricted to a specific patient (patient self-service). */
    public void viewDetailForPatient(String patientId) {
        Console.header("APPOINTMENT DETAIL");
        String id = input.readString("  Appointment ID : ").toUpperCase();
        Appointment a = store.findAppointmentById(id);
        if (a == null) { Console.error("Not found: " + id); return; }
        if (!a.getPatientId().equals(patientId)) {
            Console.error("You can only view your own appointments.");
            return;
        }
        System.out.println(a.toDetailBlock());
    }

    // =========================================================================
    // STATUS CHANGES
    // =========================================================================

    /** Cancel — usable by Admin or Patient (patient mode enforces ownership). */
    public void cancelAppointment(String ownerPatientId) {
        Console.header("CANCEL APPOINTMENT");

        String id = input.readString("  Appointment ID to cancel : ").toUpperCase();
        Appointment appt = store.findAppointmentById(id);
        if (appt == null) { Console.error("Not found: " + id); return; }

        // Ownership check for patient-role cancellation
        if (ownerPatientId != null && !appt.getPatientId().equals(ownerPatientId)) {
            Console.error("You can only cancel your own appointments.");
            return;
        }
        if (appt.getStatus() == AppointmentStatus.CANCELLED)  { Console.warn("Already cancelled."); return; }
        if (appt.getStatus() == AppointmentStatus.COMPLETED)  { Console.warn("Cannot cancel a completed appointment."); return; }

        System.out.printf("  Patient : %s%n", appt.getPatient() != null ? appt.getPatient().getName() : appt.getPatientId());
        System.out.printf("  Doctor  : Dr. %s%n", appt.getDoctor() != null ? appt.getDoctor().getName() : appt.getDoctorId());
        System.out.printf("  Date    : %s at %s%n", appt.getDate(), appt.getTimeSlot());

        if (!input.readYesNo("\n  Confirm cancellation? (y/n) : ")) { Console.info("Cancelled."); return; }

        Doctor doctor = appt.getDoctor() != null ? appt.getDoctor() : store.doctors.get(appt.getDoctorId());
        if (doctor != null) debugSlotStates("Before cancellation", doctor, appt.getDate(), appt.getTimeSlot());
        appt.setStatus(AppointmentStatus.CANCELLED);
        store.saveAppointments();
        if (doctor != null) debugSlotStates("After cancellation - slot should be free", doctor, appt.getDate(), appt.getTimeSlot());
        Console.success("Appointment " + id + " has been cancelled.");
    }

    /** Mark appointment COMPLETED — Doctor or Admin. */
    public void completeAppointment(String ownerDoctorId) {
        Console.header("MARK AS COMPLETED");

        String id = input.readString("  Appointment ID : ").toUpperCase();
        Appointment appt = store.findAppointmentById(id);
        if (appt == null)                                          { Console.error("Not found: " + id); return; }
        if (ownerDoctorId != null && !appt.getDoctorId().equals(ownerDoctorId)) {
            Console.error("You can only complete your own appointments.");
            return;
        }
        if (appt.getStatus() != AppointmentStatus.BOOKED) { Console.warn("Only BOOKED appointments can be completed."); return; }

        appt.setStatus(AppointmentStatus.COMPLETED);
        store.saveAppointments();
        Console.success("Appointment " + id + " marked as COMPLETED.");
    }

    /** Reschedule — Admin only. */
    public void rescheduleAppointment() {
        Console.header("RESCHEDULE APPOINTMENT");

        String id = input.readString("  Appointment ID : ").toUpperCase();
        Appointment appt = store.findAppointmentById(id);
        if (appt == null)                                      { Console.error("Not found: " + id); return; }
        if (appt.getStatus() != AppointmentStatus.BOOKED)     { Console.warn("Only BOOKED appointments can be rescheduled."); return; }

        System.out.printf("  Current: %s at %s with Dr. %s%n",
            appt.getDate(), appt.getTimeSlot(),
            appt.getDoctor() != null ? appt.getDoctor().getName() : appt.getDoctorId());

        Doctor newDoctor = appt.getDoctor();
        if (input.readYesNo("  Change doctor? (y/n) : ")) {
            newDoctor = promptDoctorSelect();
            if (newDoctor == null) return;
        }

        String newDate = promptDoctorAvailableDate(newDoctor, "  New Date (yyyy-MM-dd)        : ");
        String newSlot = promptSlotSelect(newDoctor, newDate);
        if (newSlot == null) { Console.warn("No available slots. Reschedule aborted."); return; }

        Doctor oldDoctor = appt.getDoctor() != null ? appt.getDoctor() : store.doctors.get(appt.getDoctorId());
        String oldDate = appt.getDate();
        String oldSlot = appt.getTimeSlot();
        if (oldDoctor != null) debugSlotStates("Before admin reschedule - old slot", oldDoctor, oldDate, oldSlot);
        debugSlotStates("Before admin reschedule - new slot reserved", newDoctor, newDate, newSlot);

        appt.setDoctorId(newDoctor.getDoctorId());
        appt.setDoctor(newDoctor);
        appt.setDate(newDate);
        appt.setTimeSlot(newSlot);

        store.saveAppointments();
        if (oldDoctor != null) debugSlotStates("After admin reschedule - old slot refreshed", oldDoctor, oldDate, oldSlot);
        debugSlotStates("After admin reschedule - new slot booked", newDoctor, newDate, newSlot);
        Console.success("Rescheduled to " + DateUtils.pretty(newDate) + " at " + newSlot);
    }

    /** Reschedule restricted to the logged-in patient's own upcoming appointment. */
    public void reschedulePatientAppointment(String ownerPatientId) {
        Console.header("RESCHEDULE MY APPOINTMENT");

        String id = input.readString("  Appointment ID : ").toUpperCase();
        Appointment appt = store.findAppointmentById(id);
        if (appt == null) { Console.error("Not found: " + id); return; }
        if (!appt.getPatientId().equals(ownerPatientId)) {
            Console.error("You can only reschedule your own appointments.");
            return;
        }
        if (appt.getStatus() != AppointmentStatus.BOOKED) {
            Console.warn("Only booked appointments can be rescheduled.");
            return;
        }
        if (isPastAppointment(appt)) {
            Console.warn("This appointment has already passed and cannot be rescheduled.");
            return;
        }

        Doctor doctor = appt.getDoctor() != null ? appt.getDoctor() : store.doctors.get(appt.getDoctorId());
        if (doctor == null) {
            Console.error("Doctor record not found for this appointment.");
            return;
        }

        System.out.printf("  Current: %s at %s with Dr. %s%n",
            DateUtils.pretty(appt.getDate()), appt.getTimeSlot(), doctor.getName());
        Console.info("Patients can reschedule the date and time, but the doctor stays the same.");

        String newDate = promptDoctorAvailableDate(doctor, "  New Date (yyyy-MM-dd)        : ");
        String newSlot = promptSlotSelect(doctor, newDate);
        if (newSlot == null) { Console.warn("No available slots. Reschedule aborted."); return; }

        Patient patient = appt.getPatient() != null ? appt.getPatient() : store.patients.get(ownerPatientId);
        String patientName = patient != null ? patient.getName() : ownerPatientId;

        Console.confirmBox(patientName, doctor.getName(),
            doctor.getDepartment().getDisplayName(), newDate, newSlot);
        if (!input.readYesNo("  Confirm reschedule? (y/n) : ")) {
            Console.info("Reschedule cancelled.");
            return;
        }

        String oldDate = appt.getDate();
        String oldSlot = appt.getTimeSlot();
        debugSlotStates("Before patient reschedule - old slot", doctor, oldDate, oldSlot);
        debugSlotStates("Before patient reschedule - new slot reserved", doctor, newDate, newSlot);

        appt.setDate(newDate);
        appt.setTimeSlot(newSlot);
        appt.setDoctor(doctor);

        store.saveAppointments();
        debugSlotStates("After patient reschedule - old slot refreshed", doctor, oldDate, oldSlot);
        debugSlotStates("After patient reschedule - new slot booked", doctor, newDate, newSlot);
        Console.success("Rescheduled to " + DateUtils.pretty(newDate) + " at " + newSlot);
    }

    // =========================================================================
    // SEARCH & FILTER
    // =========================================================================

    public void searchAll() {
        Console.header("SEARCH ALL RECORDS");
        System.out.println("  [1] Patient Name");
        System.out.println("  [2] Patient ID");
        System.out.println("  [3] Date");
        System.out.println("  [4] Doctor Name");
        System.out.println("  [5] Appointment ID");

        int choice = input.readIntInRange("\n  Search by : ", 1, 5);
        String kw  = (choice == 3)
            ? input.readAnyDate("  Date (yyyy-MM-dd) : ")
            : input.readString ("  Keyword           : ").toLowerCase();

        List<Appointment> results = switch (choice) {
            case 1 -> filterWith(a -> a.getPatient() != null && a.getPatient().getName().toLowerCase().contains(kw));
            case 2 -> filterWith(a -> a.getPatientId().toLowerCase().contains(kw));
            case 3 -> filterWith(a -> a.getDate().equals(kw));
            case 4 -> filterWith(a -> a.getDoctor() != null && a.getDoctor().getName().toLowerCase().contains(kw));
            case 5 -> filterWith(a -> a.getAppointmentId().toLowerCase().contains(kw));
            default -> Collections.emptyList();
        };

        System.out.printf("%n  Found %d result(s):%n%n", results.size());
        if (results.isEmpty()) Console.info("No matches.");
        else printTable(results, true);
    }

    public void filterByStatus() {
        Console.header("FILTER BY STATUS");
        AppointmentStatus[] statuses = AppointmentStatus.values();
        for (int i = 0; i < statuses.length; i++)
            System.out.printf("  [%d] %s%n", i + 1, statuses[i].getDisplayName());
        int choice = input.readIntInRange("\n  Select : ", 1, statuses.length);
        AppointmentStatus target = statuses[choice - 1];
        List<Appointment> list = filterWith(a -> a.getStatus() == target);
        Console.section(target.getDisplayName() + " (" + list.size() + ")");
        if (list.isEmpty()) Console.info("None found."); else printTable(list, false);
    }

    public void filterByDepartment() {
        Console.header("FILTER BY DEPARTMENT");
        Department[] depts = Department.values();
        for (int i = 0; i < depts.length; i++)
            System.out.printf("  [%2d] %-14s %s%n", i + 1, depts[i].getTag(), depts[i].getDisplayName());
        int choice = input.readIntInRange("\n  Select : ", 1, depts.length);
        Department target = depts[choice - 1];
        List<Appointment> list = filterWith(a -> a.getDoctor() != null
            && a.getDoctor().getDepartment() == target);
        Console.section(target.getDisplayName() + " (" + list.size() + ")");
        if (list.isEmpty()) Console.info("None found."); else printTable(list, false);
    }

    // =========================================================================
    // DOCTOR SCHEDULE VIEW
    // =========================================================================

    public void viewDoctorSchedule() {
        Console.header("DOCTOR SCHEDULE");
        Doctor doctor = promptDoctorSelect();
        if (doctor == null) return;
        String date = input.readAnyDate("  Date (yyyy-MM-dd) : ");
        printDoctorDayView(doctor, date);
    }

    public void viewMySchedule(Doctor doctor) {
        Console.header("MY SCHEDULE");
        String date = input.readAnyDate("  Date to view (yyyy-MM-dd) : ");
        printDoctorDayView(doctor, date);
    }

    private void printDoctorDayView(Doctor doctor, String date) {
        List<String> configuredSlots = getAvailableSlotsForDate(doctor, date);
        Map<String, Appointment> slotMap = new LinkedHashMap<>();
        for (String s : configuredSlots) slotMap.put(s, null);

        for (Appointment a : store.appointments) {
            if (a.getDoctorId().equals(doctor.getDoctorId())
                && a.getDate().equals(date)
                && a.getStatus() != AppointmentStatus.CANCELLED) {
                slotMap.putIfAbsent(a.getTimeSlot(), a);
                slotMap.put(a.getTimeSlot(), a);
            }
        }

        System.out.println();
        System.out.printf("  Dr. %-20s | %s%n", doctor.getName(), DateUtils.pretty(date));
        System.out.println("  " + "-".repeat(60));
        System.out.printf("  %-8s  %-10s  %-24s  %s%n", "Time", "Status", "Patient", "Appt ID");
        System.out.println("  " + "-".repeat(60));

        for (Map.Entry<String, Appointment> e : slotMap.entrySet()) {
            if (e.getValue() == null) {
                System.out.printf("  %-8s  %-10s  %s%n", e.getKey(), "[  FREE  ]", "--");
            } else {
                Appointment a = e.getValue();
                String pat = (a.getPatient() != null) ? a.getPatient().getName() : a.getPatientId();
                System.out.printf("  %-8s  %-10s  %-24s  %s%n",
                    e.getKey(), "[ BOOKED ]", pat, a.getAppointmentId());
            }
        }

        System.out.println("  " + "-".repeat(60));
    }

    // =========================================================================
    // REPORT
    // =========================================================================

    public void viewReport() {
        Console.header("REPORT SUMMARY");

        long total     = store.appointments.size();
        long booked    = countStatus(AppointmentStatus.BOOKED);
        long cancelled = countStatus(AppointmentStatus.CANCELLED);
        long completed = countStatus(AppointmentStatus.COMPLETED);
        long noShow    = countStatus(AppointmentStatus.NO_SHOW);

        System.out.println("  +" + "=".repeat(48) + "+");
        System.out.println("  |" + Console.center("APPOINTMENT STATISTICS", 48) + "|");
        System.out.println("  +" + "-".repeat(48) + "+");
        System.out.printf ("  |  %-32s %12d  |%n", "Total Appointments",   total);
        System.out.println("  +" + "-".repeat(48) + "+");
        System.out.printf ("  |  %-32s %12d  |%n", "[*] Booked",    booked);
        System.out.printf ("  |  %-32s %12d  |%n", "[v] Completed", completed);
        System.out.printf ("  |  %-32s %12d  |%n", "[X] Cancelled", cancelled);
        System.out.printf ("  |  %-32s %12d  |%n", "[-] No Show",   noShow);
        System.out.println("  +" + "=".repeat(48) + "+");

        if (total == 0) return;

        // Per-department table
        System.out.println();
        System.out.println("  DEPARTMENT BREAKDOWN:");
        System.out.println("  " + "-".repeat(52));
        System.out.printf ("  %-30s  %8s  %8s%n", "Department", "Count", "Share");
        System.out.println("  " + "-".repeat(52));

        store.appointments.stream()
            .collect(Collectors.groupingBy(
                a -> a.getDoctor() != null
                    ? a.getDoctor().getDepartment().getDisplayName() : "Unknown",
                Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(e -> {
                double pct = (e.getValue() * 100.0) / total;
                System.out.printf("  %-30s  %8d  %7.1f%%%n", e.getKey(), e.getValue(), pct);
            });

        // Top 5 busiest dates
        System.out.println();
        System.out.println("  TOP 5 BUSIEST DATES:");
        System.out.println("  " + "-".repeat(40));
        store.appointments.stream()
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
            .collect(Collectors.groupingBy(Appointment::getDate, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> System.out.printf("  %-14s  %d appointment(s)%n",
                DateUtils.pretty(e.getKey()), e.getValue()));
    }

    // =========================================================================
    // DOCTOR SLOT MANAGEMENT
    // =========================================================================

    public void setDoctorSlots(Doctor doctor) {
        Console.header("SET PREFERRED TIME SLOTS");
        Console.fieldLine("  Doctor", "Dr. " + doctor.getName());
        Console.fieldLine("  Default availability", "24/7 unless marked unavailable");
        System.out.println();
        System.out.println("  Set availability for:");
        System.out.println("  [1] Today");
        System.out.println("  [2] Specific Date");
        int dateChoice = input.readIntInRange("\n  Choice : ", 1, 2);
        String date = dateChoice == 1
            ? DateUtils.today()
            : input.readFutureDate("  Date to configure (yyyy-MM-dd) : ");

        Map<String, List<String>> dateSlots = parseDateSlotMap(doctor.getAvailableSlots());
        List<String> currentSlots = dateSlots.containsKey(date)
            ? dateSlots.get(date)
            : DEFAULT_SLOTS;

        Console.fieldLine("  Date", date);
        Console.fieldLine("  Current preferred slots", currentSlots.isEmpty() ? "Default 24-hour slots" : String.join(", ", currentSlots));
        debugSlotStates("Before availability update", doctor, date, null);
        System.out.println();
        System.out.println("  Hourly system slots:");
        for (int i = 0; i < DEFAULT_SLOTS.size(); i++)
            System.out.printf("  [%d] %s%n", i + 1, DEFAULT_SLOTS.get(i));

        System.out.println();
        System.out.println("  Enter slot numbers for this date only, separated by commas (e.g. 1,2,3,5).");
        System.out.println("  Press Enter for all 24 hourly slots on this date, or type default to clear preferred slots:");
        String raw = input.readOptional("  > ");

        List<String> selectedSlots;
        if (raw.equalsIgnoreCase("default")) {
            selectedSlots = Collections.emptyList();
            dateSlots.remove(date);
        } else if (raw.isEmpty()) {
            selectedSlots = new ArrayList<>(DEFAULT_SLOTS);
            dateSlots.put(date, selectedSlots);
        } else {
            selectedSlots = new ArrayList<>();
            for (String tok : raw.split(",")) {
                try {
                    int idx = Integer.parseInt(tok.trim()) - 1;
                    if (idx >= 0 && idx < DEFAULT_SLOTS.size()) {
                        selectedSlots.add(DEFAULT_SLOTS.get(idx));
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (selectedSlots.isEmpty()) {
                Console.warn("No valid slot numbers selected. Availability was not changed.");
                return;
            }
            dateSlots.put(date, selectedSlots);
        }

        doctor.setAvailableSlots(encodeScheduleConfig(dateSlots, parseUnavailableEntries(doctor.getAvailableSlots())));
        store.saveDoctors();
        debugSlotStates("After availability update", doctor, date, null);
        Console.success("Preferred schedule updated for " + date + " only.");
        Console.fieldLine("  Date", date);
        Console.fieldLine("  Preferred Slots", selectedSlots.isEmpty() ? "Default 24-hour slots" : String.join(", ", selectedSlots));
    }

    public void setDoctorUnavailable(Doctor doctor) {
        Console.header("SET DOCTOR UNAVAILABILITY");
        Console.fieldLine("  Doctor", "Dr. " + doctor.getName());
        System.out.println();
        System.out.println("  Mark unavailable for:");
        System.out.println("  [1] Today");
        System.out.println("  [2] Specific Date");
        System.out.println("  [3] Date Range");
        System.out.println("  [4] Remove Unavailability");
        int choice = input.readIntInRange("\n  Choice : ", 1, 4);

        Set<String> unavailable = parseUnavailableEntries(doctor.getAvailableSlots());
        String entry;
        if (choice == 1) {
            entry = DateUtils.today();
        } else if (choice == 2) {
            entry = input.readFutureDate("  Date (yyyy-MM-dd) : ");
        } else if (choice == 3) {
            String start = input.readFutureDate("  Start Date (yyyy-MM-dd) : ");
            String end = input.readFutureDate("  End Date (yyyy-MM-dd)   : ");
            LocalDate startDate = DateUtils.parseDate(start);
            LocalDate endDate = DateUtils.parseDate(end);
            if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
                Console.error("Invalid date range.");
                return;
            }
            entry = start + DATE_RANGE_SEPARATOR + end;
        } else {
            removeDoctorUnavailability(doctor, unavailable);
            return;
        }

        unavailable.add(entry);
        doctor.setAvailableSlots(encodeScheduleConfig(parseDateSlotMap(doctor.getAvailableSlots()), unavailable));
        store.saveDoctors();
        Console.success("Dr. " + doctor.getName() + " marked unavailable.");
        Console.fieldLine("  Unavailable", entry.replace(DATE_RANGE_SEPARATOR, " to "));
    }

    private void removeDoctorUnavailability(Doctor doctor, Set<String> unavailable) {
        if (unavailable.isEmpty()) {
            Console.info("No unavailable dates or ranges are configured.");
            return;
        }

        List<String> entries = new ArrayList<>(unavailable);
        System.out.println();
        for (int i = 0; i < entries.size(); i++) {
            System.out.printf("  [%d] %s%n", i + 1, entries.get(i).replace(DATE_RANGE_SEPARATOR, " to "));
        }
        System.out.println("  [0] Cancel");
        int choice = input.readIntInRange("\n  Remove : ", 0, entries.size());
        if (choice == 0) { Console.info("Cancelled."); return; }

        unavailable.remove(entries.get(choice - 1));
        doctor.setAvailableSlots(encodeScheduleConfig(parseDateSlotMap(doctor.getAvailableSlots()), unavailable));
        store.saveDoctors();
        Console.success("Unavailability removed.");
    }

    // =========================================================================
    // PATIENT RECORD VIEW (Doctor sees their patient's info)
    // =========================================================================

    public void viewPatientRecord(String doctorId) {
        Console.header("PATIENT RECORD LOOKUP");
        String pid = input.readString("  Patient ID or Name keyword : ").toLowerCase();

        // Find patients who have appointments with this doctor
        List<Patient> related = store.appointments.stream()
            .filter(a -> a.getDoctorId().equals(doctorId)
                && a.getStatus() != AppointmentStatus.CANCELLED)
            .map(a -> store.patients.get(a.getPatientId()))
            .filter(Objects::nonNull)
            .filter(p -> p.getPatientId().toLowerCase().contains(pid)
                      || p.getName().toLowerCase().contains(pid))
            .distinct()
            .collect(Collectors.toList());

        if (related.isEmpty()) { Console.info("No matching patients found in your records."); return; }

        for (Patient p : related) {
            System.out.println();
            System.out.println("  " + "-".repeat(50));
            Console.fieldLine("  Patient ID",        p.getPatientId());
            Console.fieldLine("  Name",              p.getName());
            Console.fieldLine("  Age",               String.valueOf(p.getAge()));
            Console.fieldLine("  Address",           p.getAddress());
            Console.fieldLine("  Contact",           p.getContactNumber());
            Console.fieldLine("  Blood Type",        p.getBloodType().isEmpty() ? "N/A" : p.getBloodType());
            Console.fieldLine("  Emergency Contact Number", p.getEmergencyContact());

            // Show their appointment history with this doctor
            System.out.println();
            System.out.println("    Appointment history with you:");
            store.appointments.stream()
                .filter(a -> a.getPatientId().equals(p.getPatientId())
                          && a.getDoctorId().equals(doctorId))
                .forEach(a -> System.out.printf("    - %s | %s %s | %s%n",
                    a.getAppointmentId(), a.getDate(), a.getTimeSlot(),
                    a.getStatus().getDisplayName()));
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Doctor promptGuidedDoctorSelect() {
        Console.section("Tell us about your concern");
        System.out.println("  Choose the option closest to what you are experiencing.");
        System.out.println("  This helps recommend the right department before choosing a doctor.");
        System.out.println();
        System.out.println("  [1] Search by symptom, body part, or keyword");
        System.out.println("  [2] Not sure - start with General Medicine");
        System.out.println("  [3] Browse departments / specializations");
        for (int i = 0; i < CONCERN_OPTIONS.length; i++) {
            ConcernOption option = CONCERN_OPTIONS[i];
            System.out.printf("  [%d] %s -> %s%n",
                i + 4, option.label, option.department.getDisplayName());
        }

        int choice = input.readIntInRange("\n  Select concern : ", 1, CONCERN_OPTIONS.length + 3);
        Department department;
        String reason;

        if (choice == 1) {
            String keyword = input.readString("  Symptom / keyword : ");
            department = recommendDepartment(keyword);
            reason = "Based on keyword: " + keyword;
        } else if (choice == 2) {
            department = Department.GENERAL_MEDICINE;
            reason = "Not sure";
        } else if (choice == 3) {
            department = promptDepartmentWithDescriptions();
            reason = "Selected department";
        } else {
            ConcernOption option = CONCERN_OPTIONS[choice - 4];
            department = option.department;
            reason = option.label;
        }

        Console.section("Recommended Specialist");
        Console.fieldLine("  Concern", reason);
        Console.fieldLine("  Department", department.getDisplayName());
        Console.fieldLine("  What they handle", departmentDescription(department));

        List<Doctor> matches = store.doctors.values().stream()
            .filter(d -> d.getDepartment() == department)
            .collect(Collectors.toList());

        if (matches.isEmpty()) {
            Console.warn("No doctor found in " + department.getDisplayName() + ". Showing all doctors instead.");
            return promptDoctorSelect();
        }

        return promptDoctorSelect(matches, "Choose from recommended doctors");
    }

    private Department promptDepartmentWithDescriptions() {
        Department[] depts = Department.values();
        System.out.println();
        for (int i = 0; i < depts.length; i++) {
            Department dept = depts[i];
            System.out.printf("  [%2d] %-24s %s%n",
                i + 1, dept.getDisplayName(), departmentDescription(dept));
        }
        return depts[input.readIntInRange("\n  Department : ", 1, depts.length) - 1];
    }

    private Department recommendDepartment(String rawKeyword) {
        String keyword = rawKeyword.toLowerCase();
        Department best = Department.GENERAL_MEDICINE;
        int bestScore = 0;

        for (Department dept : Department.values()) {
            int score = keywordScore(keyword, dept.getDisplayName())
                + keywordScore(keyword, departmentDescription(dept))
                + keywordScore(keyword, departmentKeywords(dept));
            if (score > bestScore) {
                bestScore = score;
                best = dept;
            }
        }

        for (Doctor doctor : store.doctors.values()) {
            int score = keywordScore(keyword, doctor.getSpecialization())
                + keywordScore(keyword, doctor.getDepartment().getDisplayName());
            if (score > bestScore) {
                bestScore = score;
                best = doctor.getDepartment();
            }
        }

        return best;
    }

    private int keywordScore(String keyword, String source) {
        int score = 0;
        String normalized = source.toLowerCase();
        for (String word : keyword.split("\\s+")) {
            if (word.length() >= 3 && normalized.contains(word)) score++;
        }
        return score;
    }

    private String departmentDescription(Department dept) {
        return switch (dept) {
            case CARDIOLOGY -> "Heart, chest pain, blood pressure, and circulation concerns.";
            case PEDIATRICS -> "Medical care for infants, children, and teens.";
            case ORTHOPEDICS -> "Bones, joints, muscles, back pain, sprains, and injuries.";
            case NEUROLOGY -> "Brain, nerves, headaches, seizures, numbness, and dizziness.";
            case DERMATOLOGY -> "Skin, hair, nails, rashes, acne, itching, and moles.";
            case OPHTHALMOLOGY -> "Eyes, vision changes, eye pain, redness, and retina concerns.";
            case GENERAL_MEDICINE -> "General symptoms, check-ups, unclear concerns, and first evaluation.";
            case OBSTETRICS -> "Pregnancy, menstrual concerns, and reproductive health.";
            case ONCOLOGY -> "Cancer screening, tumors, chemotherapy, and cancer care.";
            case PSYCHIATRY -> "Mood, anxiety, stress, sleep, behavior, and mental health.";
            case PULMONOLOGY -> "Lungs, breathing difficulty, cough, asthma, and chest tightness.";
            case ENDOCRINOLOGY -> "Diabetes, thyroid, hormones, metabolism, and weight-related concerns.";
        };
    }

    private String departmentKeywords(Department dept) {
        return switch (dept) {
            case CARDIOLOGY -> "heart chest pain palpitation blood pressure hypertension circulation";
            case PEDIATRICS -> "child children baby infant teen fever vaccination pediatric";
            case ORTHOPEDICS -> "bone joint muscle back knee shoulder sprain fracture injury";
            case NEUROLOGY -> "headache migraine seizure numb dizziness stroke nerve brain";
            case DERMATOLOGY -> "skin rash acne itch mole hair nail allergy eczema";
            case OPHTHALMOLOGY -> "eye vision blurry redness retina glasses pain";
            case GENERAL_MEDICINE -> "not sure general fever cough checkup pain common illness";
            case OBSTETRICS -> "pregnancy menstrual period ovary reproductive prenatal gynecology";
            case ONCOLOGY -> "cancer tumor chemotherapy lump screening oncology";
            case PSYCHIATRY -> "anxiety depression stress sleep mental mood behavior panic";
            case PULMONOLOGY -> "lung breathing asthma cough shortness breath chest tightness pneumonia";
            case ENDOCRINOLOGY -> "diabetes thyroid hormone metabolism weight sugar endocrine";
        };
    }

    /** Doctor selection list prompt. Returns null on cancel. */
    public Doctor promptDoctorSelect() {
        return promptDoctorSelect(new ArrayList<>(store.doctors.values()), "Select Doctor");
    }

    private Doctor promptDoctorSelect(List<Doctor> docs, String title) {
        Console.section(title);
        System.out.println();
        System.out.printf("  %-4s %-6s %-18s %-20s %-22s %-13s %-15s %s%n",
            "#", "ID", "Name", "Department", "Specialization", "Schedule", "Slots", "Expertise");
        System.out.println("  " + "-".repeat(140));
        for (int i = 0; i < docs.size(); i++)
            System.out.println(toGuidedDoctorRow(docs.get(i), i + 1));
        System.out.println("  " + "-".repeat(140));
        System.out.println("   0 | Cancel");

        int choice = input.readIntInRange("\n  Select doctor : ", 0, docs.size());
        if (choice == 0) { Console.info("Cancelled."); return null; }
        return docs.get(choice - 1);
    }

    private String toGuidedDoctorRow(Doctor doctor, int index) {
        return String.format("  %2d | %-5s | Dr. %-14s | %-20s | %-22s | %-13s | %-15s %s",
            index,
            doctor.getDoctorId(),
            doctor.getName(),
            fit(doctor.getDepartment().getDisplayName(), 20),
            fit(doctor.getSpecialization(), 22),
            fit(doctor.getSchedule(), 13),
            fit(slotSummary(doctor), 15),
            fit(departmentDescription(doctor.getDepartment()), 34));
    }

    private String slotSummary(Doctor doctor) {
        return "24/7 default";
    }

    private String fit(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static class ConcernOption {
        private final String label;
        private final Department department;

        ConcernOption(String label, Department department) {
            this.label = label;
            this.department = department;
        }
    }

    private static class BookingSelection {
        private final Doctor doctor;
        private final String date;
        private final String slot;

        BookingSelection(Doctor doctor, String date, String slot) {
            this.doctor = doctor;
            this.date = date;
            this.slot = slot;
        }
    }

    private BookingSelection promptPatientAvailabilityFallback(Doctor originalDoctor, String date) {
        while (true) {
            Console.warn("Selected doctor is not available at this time.");

            List<Doctor> suggestions = findBookableDoctorsBySpecialization(
                originalDoctor.getSpecialization(), date, originalDoctor.getDoctorId());
            String suggestionTitle = "Other doctors with same specialization";

            if (suggestions.isEmpty()) {
                suggestions = findBookableDoctors(originalDoctor.getDepartment(), date, originalDoctor.getDoctorId());
                suggestionTitle = "Other doctors in same department";
            }

            if (suggestions.isEmpty() && originalDoctor.getDepartment() != Department.GENERAL_MEDICINE) {
                suggestions = findBookableDoctors(Department.GENERAL_MEDICINE, date, originalDoctor.getDoctorId());
                suggestionTitle = "General Medicine fallback doctors";
            }

            if (suggestions.isEmpty()) {
                Console.info("No alternate specialist or General Medicine doctor has an open slot on " +
                    DateUtils.pretty(date) + ".");
            } else {
                printBookableDoctorSuggestions(suggestionTitle, suggestions, date);
            }

            System.out.println("  [1] Choose suggested doctor");
            System.out.println("  [2] Pick another date/time");
            System.out.println("  [0] Cancel booking");
            int choice = input.readIntInRange("\n  Choice : ", 0, 2);

            if (choice == 0) {
                Console.info("Booking cancelled.");
                return null;
            }

            if (choice == 1) {
                if (suggestions.isEmpty()) {
                    Console.warn("No suggested doctors are available for this date.");
                    continue;
                }
                Doctor suggestedDoctor = promptDoctorSelect(suggestions, "Choose Suggested Doctor");
                if (suggestedDoctor == null) continue;
                String suggestedSlot = promptSlotSelect(suggestedDoctor, date);
                if (suggestedSlot != null) {
                    return new BookingSelection(suggestedDoctor, date, suggestedSlot);
                }
                continue;
            }

            String newDate = promptDoctorAvailableDate(originalDoctor, "  New Date (yyyy-MM-dd) : ");
            String newSlot = promptSlotSelect(originalDoctor, newDate);
            if (newSlot != null) {
                return new BookingSelection(originalDoctor, newDate, newSlot);
            }
            date = newDate;
        }
    }

    private List<Doctor> findBookableDoctors(Department department, String date, String excludeDoctorId) {
        return store.doctors.values().stream()
            .filter(d -> d.getDepartment() == department)
            .filter(d -> !d.getDoctorId().equals(excludeDoctorId))
            .filter(d -> hasOpenSlotForDate(d, date))
            .collect(Collectors.toList());
    }

    private List<Doctor> findBookableDoctorsBySpecialization(String specialization, String date, String excludeDoctorId) {
        return store.doctors.values().stream()
            .filter(d -> d.getSpecialization().equalsIgnoreCase(specialization))
            .filter(d -> !d.getDoctorId().equals(excludeDoctorId))
            .filter(d -> hasOpenSlotForDate(d, date))
            .collect(Collectors.toList());
    }

    private boolean hasOpenSlotForDate(Doctor doctor, String date) {
        Set<String> taken = takenSlotsFor(doctor.getDoctorId(), date);
        return getAvailableSlotsForDate(doctor, date).stream()
            .anyMatch(slot -> !taken.contains(slot) && !isPastSlot(date, slot));
    }

    private Set<String> takenSlotsFor(String doctorId, String date) {
        return store.appointments.stream()
            .filter(a -> a.getDoctorId().equals(doctorId))
            .filter(a -> a.getDate().equals(date))
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
            .map(Appointment::getTimeSlot)
            .collect(Collectors.toSet());
    }

    private void printBookableDoctorSuggestions(String title, List<Doctor> doctors, String date) {
        Console.section(title);
        System.out.printf("  %-6s %-18s %-22s %-15s %s%n",
            "ID", "Doctor", "Specialization", "Schedule", "Open Slots");
        System.out.println("  " + "-".repeat(90));
        for (Doctor doctor : doctors) {
            Set<String> taken = takenSlotsFor(doctor.getDoctorId(), date);
            String slots = getAvailableSlotsForDate(doctor, date).stream()
                .filter(slot -> !taken.contains(slot))
                .filter(slot -> !isPastSlot(date, slot))
                .collect(Collectors.joining(", "));
            System.out.printf("  %-6s Dr. %-14s %-22s %-15s %s%n",
                doctor.getDoctorId(),
                fit(doctor.getName(), 14),
                fit(doctor.getSpecialization(), 22),
                fit(doctor.getSchedule(), 15),
                slots);
        }
    }

    private String promptDoctorAvailableDate(Doctor doctor, String prompt) {
        System.out.println("  Default availability: 24/7 unless marked unavailable.");
        while (true) {
            String date = input.readFutureDate(prompt);
            if (isDoctorAvailableOnDate(doctor, date)) return date;
            Console.warn("Dr. " + doctor.getName() + " is not available on " +
                DateUtils.pretty(date) + " because this date is marked unavailable.");
        }
    }

    private boolean isDoctorAvailableOnDate(Doctor doctor, String date) {
        return DateUtils.parseDate(date) != null && !isDateUnavailable(doctor, date);
    }

    private Set<DayOfWeek> parseScheduleDays(String schedule) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (schedule == null || schedule.trim().isEmpty()) return days;

        String normalized = schedule.trim().replace(" to ", "-").replace("/", ",");
        if (normalized.equalsIgnoreCase("daily")
            || normalized.equalsIgnoreCase("everyday")
            || normalized.equalsIgnoreCase("every day")) {
            return EnumSet.allOf(DayOfWeek.class);
        }

        for (String part : normalized.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) continue;

            if (token.contains("-")) {
                String[] bounds = token.split("-", 2);
                DayOfWeek start = parseDay(bounds[0]);
                DayOfWeek end = parseDay(bounds[1]);
                if (start != null && end != null) addDayRange(days, start, end);
                continue;
            }

            DayOfWeek day = parseDay(token);
            if (day != null) days.add(day);
        }

        return days;
    }

    private void addDayRange(Set<DayOfWeek> days, DayOfWeek start, DayOfWeek end) {
        int current = start.getValue();
        int target = end.getValue();
        while (true) {
            days.add(DayOfWeek.of(current));
            if (current == target) break;
            current = current == 7 ? 1 : current + 1;
        }
    }

    private DayOfWeek parseDay(String rawDay) {
        String day = rawDay.trim().toLowerCase();
        if (day.length() >= 3) day = day.substring(0, 3);

        return switch (day) {
            case "mon" -> DayOfWeek.MONDAY;
            case "tue" -> DayOfWeek.TUESDAY;
            case "wed" -> DayOfWeek.WEDNESDAY;
            case "thu" -> DayOfWeek.THURSDAY;
            case "fri" -> DayOfWeek.FRIDAY;
            case "sat" -> DayOfWeek.SATURDAY;
            case "sun" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private String dayName(String date) {
        LocalDate parsedDate = DateUtils.parseDate(date);
        if (parsedDate == null) return "invalid date";
        String day = parsedDate.getDayOfWeek().toString().toLowerCase();
        return day.substring(0, 1).toUpperCase() + day.substring(1);
    }

    private List<String> getAvailableSlotsForDate(Doctor doctor, String date) {
        String savedSlots = doctor.getAvailableSlots();
        if (!isDoctorAvailableOnDate(doctor, date)) return Collections.emptyList();

        Map<String, List<String>> dateSlots = parseDateSlotMap(savedSlots);
        if (dateSlots.containsKey(date) && !dateSlots.get(date).isEmpty()) return dateSlots.get(date);

        return DEFAULT_SLOTS;
    }

    private List<String> getSlotsForDateIgnoringUnavailability(Doctor doctor, String date) {
        Map<String, List<String>> dateSlots = parseDateSlotMap(doctor.getAvailableSlots());
        if (dateSlots.containsKey(date) && !dateSlots.get(date).isEmpty()) return dateSlots.get(date);
        return DEFAULT_SLOTS;
    }

    private boolean usesDateSpecificSlots(String savedSlots) {
        return savedSlots != null && savedSlots.contains(DATE_SLOTS_PREFIX);
    }

    private Map<String, List<String>> parseDateSlotMap(String savedSlots) {
        Map<String, List<String>> dateSlots = new TreeMap<>();
        if (!usesDateSpecificSlots(savedSlots)) return dateSlots;

        String body = sectionBody(savedSlots, DATE_SLOTS_PREFIX);
        for (String entry : body.split(";")) {
            if (entry.trim().isEmpty()) continue;
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) continue;

            String date = parts[0].trim();
            if (DateUtils.parseDate(date) == null) continue;

            String rawSlots = parts[1].trim();
            if (rawSlots.equals(NO_SLOTS)) {
                dateSlots.put(date, Collections.emptyList());
                continue;
            }

            List<String> slots = Arrays.stream(rawSlots.split(","))
                .map(String::trim)
                .filter(DEFAULT_SLOTS::contains)
                .distinct()
                .collect(Collectors.toList());
            dateSlots.put(date, slots);
        }

        return dateSlots;
    }

    private String encodeDateSlotMap(Map<String, List<String>> dateSlots) {
        return encodeScheduleConfig(dateSlots, Collections.emptySet());
    }

    private String encodeScheduleConfig(Map<String, List<String>> dateSlots, Set<String> unavailableEntries) {
        String dateSection = DATE_SLOTS_PREFIX;
        if (!dateSlots.isEmpty()) {
            dateSection += dateSlots.entrySet().stream()
                .map(e -> e.getKey() + "=" + (e.getValue().isEmpty() ? NO_SLOTS : String.join(",", e.getValue())))
                .collect(Collectors.joining(";"));
        }

        if (unavailableEntries == null || unavailableEntries.isEmpty()) return dateSection;
        return dateSection + SCHEDULE_SECTION_SEPARATOR + UNAVAILABLE_PREFIX +
            unavailableEntries.stream().sorted().collect(Collectors.joining(","));
    }

    private String sectionBody(String savedSlots, String prefix) {
        int start = savedSlots.indexOf(prefix);
        if (start < 0) return "";
        start += prefix.length();
        int end = savedSlots.indexOf(SCHEDULE_SECTION_SEPARATOR, start);
        return end < 0 ? savedSlots.substring(start) : savedSlots.substring(start, end);
    }

    private Set<String> parseUnavailableEntries(String savedSlots) {
        Set<String> unavailable = new TreeSet<>();
        if (savedSlots == null || !savedSlots.contains(UNAVAILABLE_PREFIX)) return unavailable;

        String body = sectionBody(savedSlots, UNAVAILABLE_PREFIX);
        for (String entry : body.split(",")) {
            String value = entry.trim();
            if (value.isEmpty()) continue;

            if (value.contains(DATE_RANGE_SEPARATOR)) {
                String[] dates = value.split("\\.\\.", 2);
                if (dates.length == 2
                    && DateUtils.parseDate(dates[0]) != null
                    && DateUtils.parseDate(dates[1]) != null) {
                    unavailable.add(value);
                }
            } else if (DateUtils.parseDate(value) != null) {
                unavailable.add(value);
            }
        }
        return unavailable;
    }

    private boolean isDateUnavailable(Doctor doctor, String date) {
        LocalDate selected = DateUtils.parseDate(date);
        if (selected == null) return true;

        for (String entry : parseUnavailableEntries(doctor.getAvailableSlots())) {
            if (entry.contains(DATE_RANGE_SEPARATOR)) {
                String[] dates = entry.split("\\.\\.", 2);
                LocalDate start = DateUtils.parseDate(dates[0]);
                LocalDate end = DateUtils.parseDate(dates[1]);
                if (start != null && end != null
                    && !selected.isBefore(start)
                    && !selected.isAfter(end)) {
                    return true;
                }
            } else if (entry.equals(date)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFutureSlotForDate(Doctor doctor, String date) {
        return getAvailableSlotsForDate(doctor, date).stream()
            .anyMatch(slot -> !isPastSlot(date, slot));
    }

    private boolean isPastSlot(String date, String slot) {
        LocalDate parsedDate = DateUtils.parseDate(date);
        if (parsedDate == null || !parsedDate.equals(LocalDate.now())) return false;

        LocalTime slotTime;
        try {
            slotTime = LocalTime.parse(slot);
        } catch (Exception e) {
            return false;
        }

        return slotTime.isBefore(LocalTime.now());
    }

    private boolean isPastAppointment(Appointment appt) {
        LocalDate apptDate = DateUtils.parseDate(appt.getDate());
        if (apptDate == null) return true;
        if (apptDate.isBefore(LocalDate.now())) return true;
        if (apptDate.isAfter(LocalDate.now())) return false;
        return isPastSlot(appt.getDate(), appt.getTimeSlot());
    }

    private void debugSlotStates(String label, Doctor doctor, String date, String selectedSlot) {
        if (!SLOT_DEBUG || doctor == null || date == null) return;

        List<String> configuredSlots = getAvailableSlotsForDate(doctor, date);
        System.out.printf("  [slot-debug] %s | Dr. %s | %s%n", label, doctor.getName(), date);
        if (configuredSlots.isEmpty()) {
            System.out.println("  [slot-debug] configured slots: none");
        }

        Set<String> seen = new HashSet<>();
        for (String slot : configuredSlots) {
            List<Appointment> active = activeAppointmentsForSlot(doctor.getDoctorId(), date, slot);
            String state;
            if (active.size() > 1) {
                state = "CONFLICT(" + active.size() + " active appointments)";
            } else if (!active.isEmpty()) {
                state = active.get(0).getStatus() == AppointmentStatus.BOOKED ? "BOOKED" : active.get(0).getStatus().name();
            } else if (isPastSlot(date, slot)) {
                state = "UNAVAILABLE_PAST";
            } else if (slot.equals(selectedSlot) && label.toLowerCase().contains("reserved")) {
                state = "RESERVED";
            } else {
                state = "FREE";
            }
            System.out.printf("  [slot-debug]   %s -> %s%n", slot, state);
            seen.add(slot);
        }

        if (selectedSlot != null && !seen.contains(selectedSlot)) {
            System.out.printf("  [slot-debug]   %s -> NOT_CONFIGURED%n", selectedSlot);
        }
        validateSlotConsistency(doctor, date, configuredSlots);
    }

    private List<Appointment> activeAppointmentsForSlot(String doctorId, String date, String slot) {
        return store.appointments.stream()
            .filter(a -> a.getDoctorId().equals(doctorId))
            .filter(a -> a.getDate().equals(date))
            .filter(a -> a.getTimeSlot().equals(slot))
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
            .collect(Collectors.toList());
    }

    private void validateSlotConsistency(Doctor doctor, String date, List<String> configuredSlots) {
        Set<String> configured = new HashSet<>(configuredSlots);
        Map<String, Long> activeCounts = store.appointments.stream()
            .filter(a -> a.getDoctorId().equals(doctor.getDoctorId()))
            .filter(a -> a.getDate().equals(date))
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
            .collect(Collectors.groupingBy(Appointment::getTimeSlot, Collectors.counting()));

        for (Map.Entry<String, Long> entry : activeCounts.entrySet()) {
            if (!configured.contains(entry.getKey())) {
                Console.warn("Data check: active appointment exists on an unconfigured slot " +
                    entry.getKey() + " for " + date + ".");
            }
            if (entry.getValue() > 1) {
                Console.warn("Data check: " + entry.getValue() + " active appointments share slot " +
                    entry.getKey() + " for Dr. " + doctor.getName() + " on " + date + ".");
            }
        }
    }

    /** Time slot selection. Returns null when no slots free or user cancels. */
    private String promptSlotSelect(Doctor doctor, String date) {
        if (!isDoctorAvailableOnDate(doctor, date)) {
            Console.warn("Dr. " + doctor.getName() + " is not available on " +
                DateUtils.pretty(date) + " (" + dayName(date) + ").");
            return null;
        }

        List<String> allSlots = getAvailableSlotsForDate(doctor, date);

        Set<String> taken = store.appointments.stream()
            .filter(a -> a.getDoctorId().equals(doctor.getDoctorId())
                      && a.getDate().equals(date)
                      && a.getStatus() != AppointmentStatus.CANCELLED)
            .map(Appointment::getTimeSlot)
            .collect(Collectors.toSet());

        if (allSlots.isEmpty()) {
            Console.warn("No date-specific time slots are configured for Dr. " +
                doctor.getName() + " on " + DateUtils.pretty(date) + ".");
            return null;
        }

        System.out.printf("%n  Available slots - Dr. %s on %s:%n",
            doctor.getName(), DateUtils.pretty(date));
        System.out.println("  " + "-".repeat(48));
        int freeIndex = 1;
        boolean hasPastSlot = false;
        Map<Integer, String> selectable = new LinkedHashMap<>();
        for (String slot : allSlots) {
            if (taken.contains(slot)) {
                System.out.printf("      %s  [BOOKED]%n", slot);
            } else if (isPastSlot(date, slot)) {
                hasPastSlot = true;
                System.out.printf("      %s  [UNAVAILABLE - PAST]%n", slot);
            } else {
                selectable.put(freeIndex, slot);
                System.out.printf("  [%d] %s  [FREE]%n", freeIndex, slot);
                freeIndex++;
            }
        }
        System.out.println("  [0] Cancel");
        System.out.println("  " + "-".repeat(48));
        if (hasPastSlot) Console.warn("Selected time has already passed.");

        if (selectable.isEmpty()) return null;

        int choice = input.readIntInRange("\n  Select slot : ", 0, selectable.size());
        if (choice == 0) { Console.info("No slot selected."); return null; }
        return selectable.get(choice);
    }

    private String promptAdminSlotSelect(Doctor doctor, String date) {
        boolean unavailable = !isDoctorAvailableOnDate(doctor, date);
        if (unavailable) {
            Console.warn("Dr. " + doctor.getName() + " is marked unavailable on " + DateUtils.pretty(date) + ".");
            if (!input.readYesNo("  Admin override and continue? (y/n) : ")) return null;
        }

        List<String> allSlots = unavailable
            ? getSlotsForDateIgnoringUnavailability(doctor, date)
            : getAvailableSlotsForDate(doctor, date);

        Set<String> taken = takenSlotsFor(doctor.getDoctorId(), date);
        System.out.printf("%n  Available slots - Dr. %s on %s:%n",
            doctor.getName(), DateUtils.pretty(date));
        System.out.println("  " + "-".repeat(48));
        int freeIndex = 1;
        Map<Integer, String> selectable = new LinkedHashMap<>();
        for (String slot : allSlots) {
            if (taken.contains(slot)) {
                System.out.printf("      %s  [BOOKED]%n", slot);
            } else if (isPastSlot(date, slot)) {
                System.out.printf("      %s  [UNAVAILABLE - PAST]%n", slot);
            } else {
                selectable.put(freeIndex, slot);
                System.out.printf("  [%d] %s  [FREE]%n", freeIndex, slot);
                freeIndex++;
            }
        }
        System.out.println("  [0] Cancel");
        System.out.println("  " + "-".repeat(48));

        if (selectable.isEmpty()) return null;

        int choice = input.readIntInRange("\n  Select slot : ", 0, selectable.size());
        if (choice == 0) { Console.info("No slot selected."); return null; }
        return selectable.get(choice);
    }

    /** Admin picks an existing patient or creates a new walk-in profile. */
    private Patient adminPickOrCreatePatient() {
        System.out.println("  [1] Existing patient (by ID)");
        System.out.println("  [2] New walk-in patient");
        int choice = input.readIntInRange("\n  Choice : ", 1, 2);

        if (choice == 1) {
            String pid = input.readString("  Patient ID : ").toUpperCase();
            Patient p  = store.patients.get(pid);
            if (p == null) { Console.error("Patient not found: " + pid); return null; }
            return p;
        }

        // Create walk-in
        Console.section("New Walk-in Patient");
        String name      = input.readFullName("  Full Name          : ");
        int    age       = input.readAge     ("  Age               : ");
        String address   = input.readString("  Address            : ");
        String contact   = input.readContact("  Contact Number     : ");
        String email     = input.readEmail  ("  Email (optional)   : ");
        String blood     = input.readBloodType("  Blood Type (opt)   : ");
        String emergency = input.readContact("  Emergency Contact Number : ");

        String pid = store.ids.nextPatientId();
        Patient p  = new Patient(pid, "", name, age, address, contact, email, blood, emergency, DateUtils.now());
        store.patients.put(pid, p);
        store.savePatients();
        Console.success("Walk-in patient created: " + pid);
        return p;
    }

    // ── Stream helpers ────────────────────────────────────────────────────────

    private List<Appointment> filterWith(java.util.function.Predicate<Appointment> pred) {
        return store.appointments.stream().filter(pred).collect(Collectors.toList());
    }

    private long countStatus(AppointmentStatus status) {
        return store.appointments.stream().filter(a -> a.getStatus() == status).count();
    }

    private void printTable(List<Appointment> list, boolean showFooter) {
        Console.appointmentTableHeader();
        for (Appointment a : list) System.out.println(a.toTableRow());
        if (showFooter) Console.appointmentTableFooter(list.size());
    }
}
