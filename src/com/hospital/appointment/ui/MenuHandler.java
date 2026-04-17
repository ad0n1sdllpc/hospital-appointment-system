package com.hospital.appointment.ui;

import com.hospital.appointment.enums.AppointmentStatus;
import com.hospital.appointment.enums.WaitlistStatus;
import com.hospital.appointment.model.Appointment;
import com.hospital.appointment.model.Doctor;
import com.hospital.appointment.model.Patient;
import com.hospital.appointment.model.WaitlistEntry;
import com.hospital.appointment.service.AppointmentManager;
import com.hospital.appointment.util.DateTimeValidator;
import com.hospital.appointment.util.InputValidator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MenuHandler {
    private final AppointmentManager appointmentManager;
    private final Scanner scanner;

    public MenuHandler(AppointmentManager appointmentManager) {
        this.appointmentManager = appointmentManager;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean running = true;

        while (running) {
            showMenu();
            int choice = readMenuChoice();

            switch (choice) {
                case 1:
                    addAppointmentFlow();
                    break;
                case 2:
                    viewAppointmentsFlow();
                    break;
                case 3:
                    cancelAppointmentFlow();
                    break;
                case 4:
                    searchAppointmentFlow();
                    break;
                case 5:
                    viewReportSummaryFlow();
                    break;
                case 6:
                    rescheduleAppointmentFlow();
                    break;
                case 7:
                    markAppointmentCompletedFlow();
                    break;
                case 8:
                    viewPatientHistoryFlow();
                    break;
                case 9:
                    viewDoctorDailyScheduleFlow();
                    break;
                case 10:
                    joinWaitlistFlow();
                    break;
                case 11:
                    viewWaitlistFlow();
                    break;
                case 12:
                    running = false;
                    System.out.println("Exiting system. Goodbye.");
                    break;
                default:
                    System.out.println("Invalid menu option.");
            }
        }
    }

    private void showMenu() {
        System.out.println();
        System.out.println("=======================================");
        System.out.println("      HOSPITAL APPOINTMENT SYSTEM");
        System.out.println("=======================================");
        System.out.println("  1. Add Appointment");
        System.out.println("  2. View Appointments");
        System.out.println("  3. Cancel Appointment");
        System.out.println("  4. Search Appointment");
        System.out.println("  5. View Report Summary");
        System.out.println("  6. Reschedule Appointment");
        System.out.println("  7. Mark Appointment Completed");
        System.out.println("  8. View Patient History");
        System.out.println("  9. View Doctor Daily Schedule");
        System.out.println(" 10. Join Waitlist");
        System.out.println(" 11. View Waitlist");
        System.out.println(" 12. Exit");
        System.out.println("=======================================");
    }

    private int readMenuChoice() {
        while (true) {
            try {
                System.out.print("Select option: ");
                String input = scanner.nextLine();
                return InputValidator.parseMenuChoice(input, 1, 12);
            } catch (IllegalArgumentException exception) {
                System.out.println("Input error: " + exception.getMessage());
            }
        }
    }

    private void addAppointmentFlow() {
        try {
            System.out.println();
            System.out.println("--- Add Appointment ---");

            String name = readNonBlank("Patient name: ");
            int age = readPositiveAge();
            String address = readNonBlank("Patient address: ");

            Doctor doctor = chooseDoctor();
            LocalDate date = readDate();
            LocalTime time = chooseAvailableTime(doctor.getDoctorId(), date);
            if (time == null) {
                if (readYesNo("No slots available. Add patient to waitlist for this doctor and date? (y/n): ")) {
                    LocalTime preferredTime = chooseWaitlistTime(doctor.getDoctorId(), date);
                    if (preferredTime != null) {
                        Patient patient = new Patient("", name, age, address);
                        WaitlistEntry entry = appointmentManager.addToWaitlist(
                                patient,
                                doctor.getDoctorId(),
                                date,
                                preferredTime
                        );
                        System.out.println("Patient added to waitlist successfully.");
                        System.out.println("Waitlist ID: " + entry.getWaitlistId());
                        System.out.println("Patient ID: " + entry.getPatient().getPatientId());
                    }
                }
                return;
            }

            Patient patient = new Patient("", name, age, address);
            Appointment appointment = appointmentManager.addAppointment(patient, doctor.getDoctorId(), date, time);

            System.out.println("Appointment booked successfully.");
            System.out.println("Appointment ID: " + appointment.getAppointmentId());
            System.out.println("Patient ID: " + appointment.getPatient().getPatientId());
        } catch (Exception exception) {
            System.out.println("Could not add appointment: " + exception.getMessage());
        }
    }

    private void joinWaitlistFlow() {
        System.out.println();
        System.out.println("--- Join Waitlist ---");

        try {
            String name = readNonBlank("Patient name: ");
            int age = readPositiveAge();
            String address = readNonBlank("Patient address: ");

            Doctor doctor = chooseDoctor();
            LocalDate date = readDate();
            LocalTime time = chooseWaitlistTime(doctor.getDoctorId(), date);
            if (time == null) {
                return;
            }

            Patient patient = new Patient("", name, age, address);
            WaitlistEntry entry = appointmentManager.addToWaitlist(patient, doctor.getDoctorId(), date, time);
            System.out.println("Patient added to waitlist successfully.");
            System.out.println("Waitlist ID: " + entry.getWaitlistId());
            System.out.println("Patient ID: " + entry.getPatient().getPatientId());
        } catch (Exception exception) {
            System.out.println("Could not add waitlist entry: " + exception.getMessage());
        }
    }

    private void viewWaitlistFlow() {
        System.out.println();
        System.out.println("--- Waitlist ---");

        List<WaitlistEntry> entries = appointmentManager.viewWaitlist();
        if (entries.isEmpty()) {
            System.out.println("No waitlist entries found.");
            return;
        }

        printWaitlistHeader();
        for (WaitlistEntry entry : entries) {
            printWaitlistRow(entry);
        }
        printWaitlistSummary(entries);
    }

    private void viewAppointmentsFlow() {
        System.out.println();
        System.out.println("--- All Appointments ---");
        List<Appointment> appointments = appointmentManager.viewAppointments();

        if (appointments.isEmpty()) {
            System.out.println("No appointments found.");
            return;
        }

        printAppointmentHeader();
        for (Appointment appointment : appointments) {
            printAppointmentRow(appointment);
        }
    }

    private void cancelAppointmentFlow() {
        System.out.println();
        System.out.println("--- Cancel Appointment ---");

        String appointmentId = readNonBlank("Enter Appointment ID: ");
        boolean cancelled = appointmentManager.cancelAppointment(appointmentId);
        if (cancelled) {
            System.out.println("Appointment cancelled successfully.");
        } else {
            System.out.println("Appointment not found or already cancelled.");
        }
    }

    private void searchAppointmentFlow() {
        System.out.println();
        System.out.println("--- Search Appointment ---");
        System.out.println("1. Search by Patient Name");
        System.out.println("2. Search by Date");
        System.out.println("3. Search by Doctor Name");

        int option;
        while (true) {
            try {
                System.out.print("Select search type: ");
                option = InputValidator.parseMenuChoice(scanner.nextLine(), 1, 3);
                break;
            } catch (IllegalArgumentException exception) {
                System.out.println("Input error: " + exception.getMessage());
            }
        }

        List<Appointment> results;
        if (option == 1) {
            String patientName = readNonBlank("Enter patient name: ");
            results = appointmentManager.searchAppointment(patientName, null, null);
        } else if (option == 2) {
            LocalDate date = readDate();
            results = appointmentManager.searchAppointment(null, date, null);
        } else {
            String doctorName = readNonBlank("Enter doctor name: ");
            results = appointmentManager.searchAppointment(null, null, doctorName);
        }

        if (results.isEmpty()) {
            System.out.println("No appointments matched your search.");
            return;
        }

        printAppointmentHeader();
        for (Appointment appointment : results) {
            printAppointmentRow(appointment);
        }
    }

    private void viewReportSummaryFlow() {
        System.out.println();
        System.out.println("--- Report Summary ---");
        Map<String, Integer> summary = appointmentManager.getReportSummary();
        int completed = summary.get("completed") == null ? 0 : summary.get("completed");

        System.out.println("Total appointments: " + summary.get("total"));
        System.out.println("Total booked: " + summary.get("booked"));
        System.out.println("Total cancelled: " + summary.get("cancelled"));
        System.out.println("Total completed: " + completed);
    }

    private void rescheduleAppointmentFlow() {
        System.out.println();
        System.out.println("--- Reschedule Appointment ---");

        String appointmentId = readNonBlank("Enter Appointment ID: ");
        Appointment current = findAppointmentById(appointmentId);
        if (current == null) {
            System.out.println("Appointment not found.");
            return;
        }

        if (current.getStatus() != AppointmentStatus.BOOKED) {
            System.out.println("Only booked appointments can be rescheduled.");
            return;
        }

        LocalDate newDate = readDate();
        LocalTime newTime = chooseAvailableTime(current.getDoctor().getDoctorId(), newDate);
        if (newTime == null) {
            return;
        }

        String fromSchedule = current.getDate() + " " + DateTimeValidator.formatTime(current.getTime());
        String toSchedule = newDate + " " + DateTimeValidator.formatTime(newTime);
        if (!readYesNo("Confirm reschedule from " + fromSchedule + " to " + toSchedule + "? (y/n): ")) {
            System.out.println("Reschedule cancelled by user.");
            return;
        }

        try {
            appointmentManager.rescheduleAppointment(appointmentId, newDate, newTime);
            System.out.println("Appointment rescheduled successfully.");
        } catch (Exception exception) {
            System.out.println("Could not reschedule appointment: " + exception.getMessage());
        }
    }

    private void markAppointmentCompletedFlow() {
        System.out.println();
        System.out.println("--- Mark Appointment Completed ---");

        String appointmentId = readNonBlank("Enter Appointment ID: ");
        Appointment current = findAppointmentById(appointmentId);
        if (current == null) {
            System.out.println("Appointment not found.");
            return;
        }

        if (current.getStatus() == AppointmentStatus.CANCELLED) {
            System.out.println("Cancelled appointments cannot be marked as completed.");
            return;
        }

        if (current.getStatus() == AppointmentStatus.COMPLETED) {
            System.out.println("Appointment is already completed.");
            return;
        }

        if (current.getDate().isAfter(LocalDate.now())) {
            System.out.println("Only appointments scheduled for today or past dates can be completed.");
            return;
        }

        if (!readYesNo("Confirm marking appointment as completed? (y/n): ")) {
            System.out.println("No changes made.");
            return;
        }

        boolean completed = appointmentManager.completeAppointment(appointmentId);
        if (completed) {
            System.out.println("Appointment marked as completed successfully.");
        } else {
            System.out.println("Could not mark appointment as completed.");
        }
    }

    private void viewPatientHistoryFlow() {
        System.out.println();
        System.out.println("--- Patient History ---");

        try {
            String patientName = readNonBlank("Enter patient name: ");
            List<Appointment> history = appointmentManager.getPatientHistory(patientName);

            if (history.isEmpty()) {
                System.out.println("No appointment history found for the provided patient name.");
                return;
            }

            printAppointmentHeader();
            for (Appointment appointment : history) {
                printAppointmentRow(appointment);
            }
            printStatusSummary(history);
        } catch (Exception exception) {
            System.out.println("Could not load patient history: " + exception.getMessage());
        }
    }

    private void viewDoctorDailyScheduleFlow() {
        System.out.println();
        System.out.println("--- Doctor Daily Schedule ---");

        try {
            Doctor doctor = chooseDoctor();
            LocalDate date = readDateAllowPast("Schedule date (yyyy-MM-dd): ");
            List<Appointment> schedule = appointmentManager.getDoctorDailySchedule(doctor.getDoctorId(), date);

            if (schedule.isEmpty()) {
                System.out.println("No schedule entries for selected doctor and date.");
                return;
            }

            printAppointmentHeader();
            for (Appointment appointment : schedule) {
                printAppointmentRow(appointment);
            }
            printStatusSummary(schedule);
        } catch (Exception exception) {
            System.out.println("Could not load doctor schedule: " + exception.getMessage());
        }
    }

    private Doctor chooseDoctor() {
        while (true) {
            System.out.println();
            System.out.println("Available doctors:");
            List<Doctor> doctors = appointmentManager.getDoctors();
            for (Doctor doctor : doctors) {
                System.out.println(doctor.getDoctorId() + " - " + doctor.getName() + " (" + doctor.getDepartment() + ")");
            }

            String doctorId = readNonBlank("Select doctor ID: ");
            for (Doctor doctor : doctors) {
                if (doctor.getDoctorId().equalsIgnoreCase(doctorId)) {
                    return doctor;
                }
            }

            System.out.println("Invalid doctor ID.");
        }
    }

    private LocalDate readDate() {
        while (true) {
            try {
                String dateInput = readNonBlank("Appointment date (yyyy-MM-dd): ");
                LocalDate date = DateTimeValidator.parseDate(dateInput);
                DateTimeValidator.ensureNotPast(date);
                return date;
            } catch (IllegalArgumentException exception) {
                System.out.println("Date error: " + exception.getMessage());
            }
        }
    }

    private LocalDate readDateAllowPast(String prompt) {
        while (true) {
            try {
                String dateInput = readNonBlank(prompt);
                return DateTimeValidator.parseDate(dateInput);
            } catch (IllegalArgumentException exception) {
                System.out.println("Date error: " + exception.getMessage());
            }
        }
    }

    private LocalTime chooseAvailableTime(String doctorId, LocalDate date) {
        List<LocalTime> availableSlots = appointmentManager.getAvailableTimeSlots(doctorId, date);

        if (availableSlots.isEmpty()) {
            System.out.println("No available time slots for selected doctor and date.");
            return null;
        }

        System.out.println("Available time slots:");
        for (int index = 0; index < availableSlots.size(); index++) {
            System.out.println((index + 1) + ". " + DateTimeValidator.formatTime(availableSlots.get(index)));
        }

        while (true) {
            try {
                System.out.print("Choose slot number: ");
                int choice = InputValidator.parseMenuChoice(scanner.nextLine(), 1, availableSlots.size());
                return availableSlots.get(choice - 1);
            } catch (IllegalArgumentException exception) {
                System.out.println("Input error: " + exception.getMessage());
            }
        }
    }

    private LocalTime chooseWaitlistTime(String doctorId, LocalDate date) {
        List<LocalTime> availableSlots = appointmentManager.getAvailableTimeSlots(doctorId, date);
        List<LocalTime> preferredSlots = new java.util.ArrayList<LocalTime>();

        for (LocalTime slot : appointmentManager.getTimeSlots()) {
            if (!availableSlots.contains(slot)) {
                preferredSlots.add(slot);
            }
        }

        if (preferredSlots.isEmpty()) {
            System.out.println("No occupied slots found. At least one time slot is still available for booking.");
            return null;
        }

        System.out.println("Booked slots available for waitlist:");
        for (int index = 0; index < preferredSlots.size(); index++) {
            System.out.println((index + 1) + ". " + DateTimeValidator.formatTime(preferredSlots.get(index)));
        }

        while (true) {
            try {
                System.out.print("Choose preferred booked slot number: ");
                int choice = InputValidator.parseMenuChoice(scanner.nextLine(), 1, preferredSlots.size());
                return preferredSlots.get(choice - 1);
            } catch (IllegalArgumentException exception) {
                System.out.println("Input error: " + exception.getMessage());
            }
        }
    }

    private int readPositiveAge() {
        while (true) {
            try {
                System.out.print("Patient age: ");
                String input = scanner.nextLine();
                return InputValidator.parsePositiveAge(input);
            } catch (IllegalArgumentException exception) {
                System.out.println("Input error: " + exception.getMessage());
            }
        }
    }

    private String readNonBlank(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine();
                InputValidator.requireNonBlank(input, "Value");
                return input.trim();
            } catch (IllegalArgumentException exception) {
                System.out.println("Input error: " + exception.getMessage());
            }
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            if ("y".equals(input) || "yes".equals(input)) {
                return true;
            }
            if ("n".equals(input) || "no".equals(input)) {
                return false;
            }
            System.out.println("Input error: Please type y/yes or n/no.");
        }
    }

    private Appointment findAppointmentById(String appointmentId) {
        for (Appointment appointment : appointmentManager.viewAppointments()) {
            if (appointment.getAppointmentId().equalsIgnoreCase(appointmentId.trim())) {
                return appointment;
            }
        }
        return null;
    }

    private void printStatusSummary(List<Appointment> appointments) {
        int booked = 0;
        int cancelled = 0;
        int completed = 0;

        for (Appointment appointment : appointments) {
            if (appointment.getStatus() == AppointmentStatus.BOOKED) {
                booked++;
            } else if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                cancelled++;
            } else if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                completed++;
            }
        }

        System.out.println("Summary -> Total: " + appointments.size()
                + ", Booked: " + booked
                + ", Cancelled: " + cancelled
                + ", Completed: " + completed);
    }

    private void printWaitlistHeader() {
        System.out.println("=================================================================================================================================================");
        System.out.printf("| %-12s | %-18s | %-3s | %-18s | %-18s | %-10s | %-8s | %-11s | %-13s |%n",
                "Waitlist ID",
                "Patient",
                "Age",
                "Address",
                "Doctor(Dept)",
                "Date",
                "Time",
                "Status",
                "AutoFill App");
        System.out.println("=================================================================================================================================================");
    }

    private void printWaitlistRow(WaitlistEntry entry) {
        String doctorLabel = entry.getDoctor().getName() + "(" + entry.getDoctor().getDepartment() + ")";
        System.out.printf("| %-12s | %-18s | %-3d | %-18s | %-18s | %-10s | %-8s | %-11s | %-13s |%n",
                entry.getWaitlistId(),
                truncate(entry.getPatient().getName(), 18),
                entry.getPatient().getAge(),
                truncate(entry.getPatient().getAddress(), 18),
                truncate(doctorLabel, 18),
                entry.getDate(),
                DateTimeValidator.formatTime(entry.getTime()),
                entry.getStatus(),
                truncate(entry.getFulfilledAppointmentId(), 13));
    }

    private void printWaitlistSummary(List<WaitlistEntry> entries) {
        int waiting = 0;
        int autoFilled = 0;
        int cancelled = 0;

        for (WaitlistEntry entry : entries) {
            if (entry.getStatus() == WaitlistStatus.WAITING) {
                waiting++;
            } else if (entry.getStatus() == WaitlistStatus.AUTO_FILLED) {
                autoFilled++;
            } else if (entry.getStatus() == WaitlistStatus.CANCELLED) {
                cancelled++;
            }
        }

        System.out.println("Waitlist Summary -> Total: " + entries.size()
                + ", Waiting: " + waiting
                + ", Auto-Filled: " + autoFilled
                + ", Cancelled: " + cancelled);
    }

    private void printAppointmentHeader() {
        System.out.println("=====================================================================================================================================");
        System.out.printf("| %-13s | %-18s | %-4s | %-18s | %-18s | %-12s | %-8s | %-10s |%n",
                "App.ID",
                "Patient",
                "Age",
                "Address",
                "Doctor(Dept)",
                "Date",
                "Time",
                "Status");
        System.out.println("=====================================================================================================================================");
    }

    private void printAppointmentRow(Appointment appointment) {
        String doctorLabel = appointment.getDoctor().getName() + "(" + appointment.getDoctor().getDepartment() + ")";
        System.out.printf("| %-13s | %-18s | %-4d | %-18s | %-18s | %-12s | %-8s | %-10s |%n",
                appointment.getAppointmentId(),
                truncate(appointment.getPatient().getName(), 18),
                appointment.getPatient().getAge(),
                truncate(appointment.getPatient().getAddress(), 18),
                truncate(doctorLabel, 18),
                appointment.getDate(),
                DateTimeValidator.formatTime(appointment.getTime()),
                appointment.getStatus());
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}