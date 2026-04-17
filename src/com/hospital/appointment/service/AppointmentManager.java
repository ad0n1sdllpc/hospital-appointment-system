package com.hospital.appointment.service;

import com.hospital.appointment.enums.AppointmentStatus;
import com.hospital.appointment.enums.Department;
import com.hospital.appointment.enums.WaitlistStatus;
import com.hospital.appointment.model.Appointment;
import com.hospital.appointment.model.Doctor;
import com.hospital.appointment.model.Patient;
import com.hospital.appointment.model.WaitlistEntry;
import com.hospital.appointment.storage.FileHandler;
import com.hospital.appointment.util.DateTimeValidator;
import com.hospital.appointment.util.IdGenerator;
import com.hospital.appointment.util.InputValidator;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentManager implements AppointmentService {
    private final List<Appointment> appointments;
    private final List<WaitlistEntry> waitlistEntries;
    private final List<Doctor> doctors;
    private final List<LocalTime> timeSlots;
    private final FileHandler fileHandler;
    private final IdGenerator idGenerator;

    public AppointmentManager(FileHandler fileHandler) {
        this.appointments = new ArrayList<Appointment>();
        this.waitlistEntries = new ArrayList<WaitlistEntry>();
        this.doctors = new ArrayList<Doctor>();
        this.timeSlots = new ArrayList<LocalTime>();
        this.fileHandler = fileHandler;
        this.idGenerator = new IdGenerator();

        initializeDoctors();
        initializeTimeSlots();
        loadExistingAppointments();
        loadExistingWaitlist();
        idGenerator.syncFromAppointments(appointments);
        idGenerator.syncFromWaitlist(waitlistEntries);
        idGenerator.syncPatientCount(appointments.size() + waitlistEntries.size());
    }

    @Override
    public Appointment addAppointment(Patient patient, String doctorId, LocalDate date, LocalTime time) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient data is required.");
        }

        InputValidator.requireNonBlank(patient.getName(), "Patient name");
        if (patient.getAge() <= 0) {
            throw new IllegalArgumentException("Age must be a positive number.");
        }
        InputValidator.requireNonBlank(patient.getAddress(), "Patient address");
        InputValidator.requireNonBlank(doctorId, "Doctor ID");
        DateTimeValidator.ensureNotPast(date);

        Doctor doctor = findDoctorById(doctorId);
        if (doctor == null) {
            throw new IllegalArgumentException("Doctor not found for ID: " + doctorId);
        }

        if (!timeSlots.contains(time)) {
            throw new IllegalArgumentException("Invalid time slot.");
        }

        if (isDuplicateActiveSchedule(doctorId, date, time)) {
            throw new IllegalStateException("Selected doctor is already booked for that date and time.");
        }

        if (patient.getPatientId() == null || patient.getPatientId().trim().isEmpty()) {
            patient.setPatientId(idGenerator.nextPatientId());
        }

        String appointmentId = idGenerator.nextAppointmentId(date.getYear());
        Appointment appointment = new Appointment(
                appointmentId,
                patient,
                doctor,
                date,
                time,
                AppointmentStatus.BOOKED
        );

        appointments.add(appointment);
        autoSave();
        return appointment;
    }

    @Override
    public List<Appointment> viewAppointments() {
        List<Appointment> snapshot = new ArrayList<Appointment>(appointments);
        Collections.sort(snapshot, new Comparator<Appointment>() {
            @Override
            public int compare(Appointment first, Appointment second) {
                int dateCompare = first.getDate().compareTo(second.getDate());
                if (dateCompare != 0) {
                    return dateCompare;
                }
                return first.getTime().compareTo(second.getTime());
            }
        });
        return snapshot;
    }

    @Override
    public boolean cancelAppointment(String appointmentId) {
        InputValidator.requireNonBlank(appointmentId, "Appointment ID");

        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentId().equalsIgnoreCase(appointmentId.trim())) {
                if (appointment.getStatus() != AppointmentStatus.BOOKED) {
                    return false;
                }
                appointment.setStatus(AppointmentStatus.CANCELLED);
                processWaitlistForCancelledSlot(
                        appointment.getDoctor().getDoctorId(),
                        appointment.getDate(),
                        appointment.getTime()
                );
                autoSave();
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Appointment> searchAppointment(String patientName, LocalDate date, String doctorName) {
        String patientKeyword = InputValidator.safeLower(patientName);
        String doctorKeyword = InputValidator.safeLower(doctorName);

        List<Appointment> matches = new ArrayList<Appointment>();
        for (Appointment appointment : appointments) {
            boolean patientMatches = patientKeyword.isEmpty() ||
                    appointment.getPatient().getName().toLowerCase().contains(patientKeyword);

            boolean dateMatches = date == null || appointment.getDate().equals(date);

            boolean doctorMatches = doctorKeyword.isEmpty() ||
                    appointment.getDoctor().getName().toLowerCase().contains(doctorKeyword);

            if (patientMatches && dateMatches && doctorMatches) {
                matches.add(appointment);
            }
        }

        return matches;
    }

    @Override
    public Appointment rescheduleAppointment(String appointmentId, LocalDate newDate, LocalTime newTime) {
        InputValidator.requireNonBlank(appointmentId, "Appointment ID");
        if (newDate == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        if (newTime == null) {
            throw new IllegalArgumentException("Time is required.");
        }
        DateTimeValidator.ensureNotPast(newDate);

        Appointment appointment = findAppointmentById(appointmentId);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment not found for ID: " + appointmentId);
        }

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new IllegalStateException("Only booked appointments can be rescheduled.");
        }

        if (!timeSlots.contains(newTime)) {
            throw new IllegalArgumentException("Invalid time slot.");
        }

        String doctorId = appointment.getDoctor().getDoctorId();
        if (isDuplicateActiveScheduleExceptAppointment(doctorId, newDate, newTime, appointment.getAppointmentId())) {
            throw new IllegalStateException("Selected doctor is already booked for that date and time.");
        }

        appointment.setDate(newDate);
        appointment.setTime(newTime);
        autoSave();
        return appointment;
    }

    @Override
    public boolean completeAppointment(String appointmentId) {
        InputValidator.requireNonBlank(appointmentId, "Appointment ID");

        Appointment appointment = findAppointmentById(appointmentId);
        if (appointment == null) {
            return false;
        }

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            return false;
        }

        if (appointment.getDate().isAfter(LocalDate.now())) {
            return false;
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        autoSave();
        return true;
    }

    @Override
    public List<Appointment> getPatientHistory(String patientName) {
        String patientKeyword = InputValidator.safeLower(patientName);
        if (patientKeyword.isEmpty()) {
            throw new IllegalArgumentException("Patient name is required.");
        }

        List<Appointment> history = new ArrayList<Appointment>();
        for (Appointment appointment : appointments) {
            if (appointment.getPatient().getName().toLowerCase().contains(patientKeyword)) {
                history.add(appointment);
            }
        }

        Collections.sort(history, new Comparator<Appointment>() {
            @Override
            public int compare(Appointment first, Appointment second) {
                int dateCompare = second.getDate().compareTo(first.getDate());
                if (dateCompare != 0) {
                    return dateCompare;
                }
                return second.getTime().compareTo(first.getTime());
            }
        });

        return history;
    }

    @Override
    public List<Appointment> getDoctorDailySchedule(String doctorId, LocalDate date) {
        InputValidator.requireNonBlank(doctorId, "Doctor ID");
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        Doctor doctor = findDoctorById(doctorId);
        if (doctor == null) {
            throw new IllegalArgumentException("Doctor not found for ID: " + doctorId);
        }

        List<Appointment> schedule = new ArrayList<Appointment>();
        for (Appointment appointment : appointments) {
            boolean sameDoctor = appointment.getDoctor().getDoctorId().equalsIgnoreCase(doctor.getDoctorId());
            boolean sameDate = appointment.getDate().equals(date);
            if (sameDoctor && sameDate) {
                schedule.add(appointment);
            }
        }

        Collections.sort(schedule, new Comparator<Appointment>() {
            @Override
            public int compare(Appointment first, Appointment second) {
                return first.getTime().compareTo(second.getTime());
            }
        });

        return schedule;
    }

    @Override
    public WaitlistEntry addToWaitlist(Patient patient, String doctorId, LocalDate date, LocalTime time) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient data is required.");
        }

        InputValidator.requireNonBlank(patient.getName(), "Patient name");
        if (patient.getAge() <= 0) {
            throw new IllegalArgumentException("Age must be a positive number.");
        }
        InputValidator.requireNonBlank(patient.getAddress(), "Patient address");
        InputValidator.requireNonBlank(doctorId, "Doctor ID");
        DateTimeValidator.ensureNotPast(date);
        if (time == null) {
            throw new IllegalArgumentException("Time is required.");
        }

        Doctor doctor = findDoctorById(doctorId);
        if (doctor == null) {
            throw new IllegalArgumentException("Doctor not found for ID: " + doctorId);
        }

        if (!timeSlots.contains(time)) {
            throw new IllegalArgumentException("Invalid time slot.");
        }

        List<LocalTime> availableSlots = getAvailableTimeSlots(doctorId, date);
        if (availableSlots.contains(time)) {
            throw new IllegalStateException("Preferred slot is still available. Book directly instead of waitlist.");
        }

        if (isDuplicateActiveWaitlist(patient, doctorId, date, time)) {
            throw new IllegalStateException("Patient already has an active waitlist entry for the selected slot.");
        }

        if (patient.getPatientId() == null || patient.getPatientId().trim().isEmpty()) {
            patient.setPatientId(idGenerator.nextPatientId());
        }

        String waitlistId = idGenerator.nextWaitlistId(date.getYear());
        WaitlistEntry entry = new WaitlistEntry(
                waitlistId,
                patient,
                doctor,
                date,
                time,
                WaitlistStatus.WAITING,
                LocalDateTime.now(),
                ""
        );

        waitlistEntries.add(entry);
        autoSave();
        return entry;
    }

    @Override
    public List<WaitlistEntry> viewWaitlist() {
        List<WaitlistEntry> snapshot = new ArrayList<WaitlistEntry>(waitlistEntries);
        Collections.sort(snapshot, new Comparator<WaitlistEntry>() {
            @Override
            public int compare(WaitlistEntry first, WaitlistEntry second) {
                if (first.getStatus() != second.getStatus()) {
                    if (first.getStatus() == WaitlistStatus.WAITING) {
                        return -1;
                    }
                    if (second.getStatus() == WaitlistStatus.WAITING) {
                        return 1;
                    }
                }
                return first.getCreatedAt().compareTo(second.getCreatedAt());
            }
        });
        return snapshot;
    }

    @Override
    public boolean cancelWaitlistEntry(String waitlistId) {
        InputValidator.requireNonBlank(waitlistId, "Waitlist ID");

        for (WaitlistEntry entry : waitlistEntries) {
            if (!entry.getWaitlistId().equalsIgnoreCase(waitlistId.trim())) {
                continue;
            }

            if (entry.getStatus() != WaitlistStatus.WAITING) {
                return false;
            }

            entry.setStatus(WaitlistStatus.CANCELLED);
            autoSave();
            return true;
        }

        return false;
    }

    @Override
    public Map<String, Integer> getReportSummary() {
        int total = appointments.size();
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

        Map<String, Integer> summary = new HashMap<String, Integer>();
        summary.put("total", total);
        summary.put("booked", booked);
        summary.put("cancelled", cancelled);
        summary.put("completed", completed);
        return summary;
    }

    @Override
    public List<Doctor> getDoctors() {
        return new ArrayList<Doctor>(doctors);
    }

    @Override
    public List<LocalTime> getAvailableTimeSlots(String doctorId, LocalDate date) {
        InputValidator.requireNonBlank(doctorId, "Doctor ID");
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        List<LocalTime> available = new ArrayList<LocalTime>(timeSlots);

        for (Appointment appointment : appointments) {
            boolean sameDoctor = appointment.getDoctor().getDoctorId().equalsIgnoreCase(doctorId.trim());
            boolean sameDate = appointment.getDate().equals(date);
            boolean isBooked = appointment.getStatus() == AppointmentStatus.BOOKED;

            if (sameDoctor && sameDate && isBooked) {
                available.remove(appointment.getTime());
            }
        }

        return available;
    }

    @Override
    public List<LocalTime> getTimeSlots() {
        return new ArrayList<LocalTime>(timeSlots);
    }

    private void initializeDoctors() {
        doctors.add(new Doctor("D-01", "Dr. Smith", Department.CARDIOLOGY));
        doctors.add(new Doctor("D-02", "Dr. Patel", Department.PEDIATRICS));
        doctors.add(new Doctor("D-03", "Dr. Lee", Department.ORTHOPEDICS));
        doctors.add(new Doctor("D-04", "Dr. Garcia", Department.DERMATOLOGY));
        doctors.add(new Doctor("D-05", "Dr. Cruz", Department.NEUROLOGY));
    }

    private void initializeTimeSlots() {
        timeSlots.add(LocalTime.of(9, 0));
        timeSlots.add(LocalTime.of(10, 0));
        timeSlots.add(LocalTime.of(11, 0));
        timeSlots.add(LocalTime.of(13, 0));
        timeSlots.add(LocalTime.of(14, 0));
        timeSlots.add(LocalTime.of(15, 0));
    }

    private void loadExistingAppointments() {
        try {
            appointments.addAll(fileHandler.loadFromFile(doctors));
        } catch (IOException exception) {
            System.out.println("Could not load existing appointments: " + exception.getMessage());
        }
    }

    private void loadExistingWaitlist() {
        try {
            waitlistEntries.addAll(fileHandler.loadWaitlistFromFile(doctors));
        } catch (IOException exception) {
            System.out.println("Could not load existing waitlist: " + exception.getMessage());
        }
    }

    private Doctor findDoctorById(String doctorId) {
        for (Doctor doctor : doctors) {
            if (doctor.getDoctorId().equalsIgnoreCase(doctorId.trim())) {
                return doctor;
            }
        }
        return null;
    }

    private boolean isDuplicateActiveSchedule(String doctorId, LocalDate date, LocalTime time) {
        for (Appointment appointment : appointments) {
            boolean sameDoctor = appointment.getDoctor().getDoctorId().equalsIgnoreCase(doctorId.trim());
            boolean sameDate = appointment.getDate().equals(date);
            boolean sameTime = appointment.getTime().equals(time);
            boolean active = appointment.getStatus() == AppointmentStatus.BOOKED;

            if (sameDoctor && sameDate && sameTime && active) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicateActiveScheduleExceptAppointment(String doctorId, LocalDate date, LocalTime time,
                                                               String appointmentIdToIgnore) {
        for (Appointment appointment : appointments) {
            boolean sameId = appointment.getAppointmentId().equalsIgnoreCase(appointmentIdToIgnore.trim());
            if (sameId) {
                continue;
            }

            boolean sameDoctor = appointment.getDoctor().getDoctorId().equalsIgnoreCase(doctorId.trim());
            boolean sameDate = appointment.getDate().equals(date);
            boolean sameTime = appointment.getTime().equals(time);
            boolean active = appointment.getStatus() == AppointmentStatus.BOOKED;

            if (sameDoctor && sameDate && sameTime && active) {
                return true;
            }
        }
        return false;
    }

    private Appointment findAppointmentById(String appointmentId) {
        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentId().equalsIgnoreCase(appointmentId.trim())) {
                return appointment;
            }
        }
        return null;
    }

    private boolean isDuplicateActiveWaitlist(Patient patient, String doctorId, LocalDate date, LocalTime time) {
        for (WaitlistEntry entry : waitlistEntries) {
            if (entry.getStatus() != WaitlistStatus.WAITING) {
                continue;
            }

            boolean sameDoctor = entry.getDoctor().getDoctorId().equalsIgnoreCase(doctorId.trim());
            boolean sameDate = entry.getDate().equals(date);
            boolean sameTime = entry.getTime().equals(time);
            boolean samePatient = isSamePatient(entry.getPatient(), patient);

            if (sameDoctor && sameDate && sameTime && samePatient) {
                return true;
            }
        }
        return false;
    }

    private boolean isSamePatient(Patient first, Patient second) {
        String firstId = first.getPatientId() == null ? "" : first.getPatientId().trim();
        String secondId = second.getPatientId() == null ? "" : second.getPatientId().trim();

        if (!firstId.isEmpty() && !secondId.isEmpty()) {
            return firstId.equalsIgnoreCase(secondId);
        }

        boolean sameName = first.getName().trim().equalsIgnoreCase(second.getName().trim());
        boolean sameAge = first.getAge() == second.getAge();
        boolean sameAddress = first.getAddress().trim().equalsIgnoreCase(second.getAddress().trim());
        return sameName && sameAge && sameAddress;
    }

    private void processWaitlistForCancelledSlot(String doctorId, LocalDate date, LocalTime time) {
        for (WaitlistEntry entry : waitlistEntries) {
            if (entry.getStatus() != WaitlistStatus.WAITING) {
                continue;
            }

            boolean sameDoctor = entry.getDoctor().getDoctorId().equalsIgnoreCase(doctorId.trim());
            boolean sameDate = entry.getDate().equals(date);
            boolean sameTime = entry.getTime().equals(time);

            if (!sameDoctor || !sameDate || !sameTime) {
                continue;
            }

            try {
                Appointment autoFilled = addAppointment(
                        entry.getPatient(),
                        entry.getDoctor().getDoctorId(),
                        entry.getDate(),
                        entry.getTime()
                );
                entry.setStatus(WaitlistStatus.AUTO_FILLED);
                entry.setFulfilledAppointmentId(autoFilled.getAppointmentId());
                autoSave();
                return;
            } catch (Exception ignored) {
                // If auto-fill fails for this slot, keep scanning for another eligible waiting entry.
            }
        }
    }

    private void autoSave() {
        try {
            fileHandler.saveToFile(appointments);
            fileHandler.saveWaitlistToFile(waitlistEntries);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to save records: " + exception.getMessage(), exception);
        }
    }
}