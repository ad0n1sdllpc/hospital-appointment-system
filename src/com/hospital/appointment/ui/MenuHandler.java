package com.hospital.appointment.ui;

import com.hospital.appointment.model.Appointment;
import com.hospital.appointment.model.Doctor;
import com.hospital.appointment.model.Patient;
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
        System.out.println("  6. Exit");
        System.out.println("=======================================");
    }

    private int readMenuChoice() {
        while (true) {
            try {
                System.out.print("Select option: ");
                String input = scanner.nextLine();
                return InputValidator.parseMenuChoice(input, 1, 6);
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

        System.out.println("Total appointments: " + summary.get("total"));
        System.out.println("Total booked: " + summary.get("booked"));
        System.out.println("Total cancelled: " + summary.get("cancelled"));
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