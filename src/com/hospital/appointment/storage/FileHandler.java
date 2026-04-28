package com.hospital.appointment.storage;

import com.hospital.appointment.enums.AppointmentStatus;
import com.hospital.appointment.enums.Department;
import com.hospital.appointment.enums.WaitlistStatus;
import com.hospital.appointment.model.Appointment;
import com.hospital.appointment.model.Doctor;
import com.hospital.appointment.model.Patient;
import com.hospital.appointment.model.WaitlistEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final Path appointmentFilePath;
    private final Path waitlistFilePath;

    public FileHandler(String appointmentFilePath) {
        this(appointmentFilePath, deriveWaitlistPath(appointmentFilePath));
    }

    public FileHandler(String appointmentFilePath, String waitlistFilePath) {
        this.appointmentFilePath = Paths.get(appointmentFilePath);
        this.waitlistFilePath = Paths.get(waitlistFilePath);
    }

    public void saveToFile(List<Appointment> appointments) throws IOException {
        List<String> lines = new ArrayList<String>();
        for (Appointment appointment : appointments) {
            String line = String.join("|",
                    appointment.getAppointmentId(),
                    appointment.getPatient().getName(),
                    String.valueOf(appointment.getPatient().getAge()),
                    appointment.getPatient().getAddress(),
                    appointment.getDoctor().getDoctorId(),
                    appointment.getDoctor().getName(),
                    appointment.getDoctor().getDepartment().name(),
                    appointment.getDate().toString(),
                    appointment.getTime().format(TIME_FORMATTER),
                    appointment.getStatus().name()
            );
            lines.add(line);
        }

        if (appointmentFilePath.getParent() != null) {
            Files.createDirectories(appointmentFilePath.getParent());
        }

        Files.write(appointmentFilePath, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    public List<Appointment> loadFromFile(List<Doctor> doctors) throws IOException {
        List<Appointment> loaded = new ArrayList<Appointment>();

        if (!Files.exists(appointmentFilePath)) {
            if (appointmentFilePath.getParent() != null) {
                Files.createDirectories(appointmentFilePath.getParent());
            }
            Files.createFile(appointmentFilePath);
            return loaded;
        }

        List<String> lines = Files.readAllLines(appointmentFilePath, StandardCharsets.UTF_8);
        int syntheticPatientCounter = 1;

        for (String rawLine : lines) {
            if (rawLine == null || rawLine.trim().isEmpty()) {
                continue;
            }

            String[] parts = rawLine.split("\\|");
            if (parts.length != 10) {
                continue;
            }

            String appointmentId = parts[0].trim();
            String patientName = parts[1].trim();
            int patientAge = Integer.parseInt(parts[2].trim());
            String patientAddress = parts[3].trim();
            String doctorId = parts[4].trim();
            String doctorName = parts[5].trim();
            Department department = Department.fromText(parts[6].trim());
            LocalDate date = LocalDate.parse(parts[7].trim());
            LocalTime time = LocalTime.parse(parts[8].trim(), TIME_FORMATTER);
            AppointmentStatus status = AppointmentStatus.valueOf(parts[9].trim().toUpperCase());

            Doctor doctor = findOrCreateDoctor(doctors, doctorId, doctorName, department);
            String syntheticPatientId = String.format("PAT-%03d", syntheticPatientCounter++);
            Patient patient = new Patient(syntheticPatientId, patientName, patientAge, patientAddress);

            Appointment appointment = new Appointment(
                    appointmentId,
                    patient,
                    doctor,
                    date,
                    time,
                    status
            );

            loaded.add(appointment);
        }

        return loaded;
    }

    public void saveWaitlistToFile(List<WaitlistEntry> waitlistEntries) throws IOException {
        List<String> lines = new ArrayList<String>();
        for (WaitlistEntry entry : waitlistEntries) {
            String line = String.join("|",
                    entry.getWaitlistId(),
                    entry.getPatient().getPatientId(),
                    entry.getPatient().getName(),
                    String.valueOf(entry.getPatient().getAge()),
                    entry.getPatient().getAddress(),
                    entry.getDoctor().getDoctorId(),
                    entry.getDoctor().getName(),
                    entry.getDoctor().getDepartment().name(),
                    entry.getDate().toString(),
                    entry.getTime().format(TIME_FORMATTER),
                    entry.getStatus().name(),
                    entry.getCreatedAt().toString(),
                    entry.getFulfilledAppointmentId()
            );
            lines.add(line);
        }

        if (waitlistFilePath.getParent() != null) {
            Files.createDirectories(waitlistFilePath.getParent());
        }

        Files.write(waitlistFilePath, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    public List<WaitlistEntry> loadWaitlistFromFile(List<Doctor> doctors) throws IOException {
        List<WaitlistEntry> loaded = new ArrayList<WaitlistEntry>();

        if (!Files.exists(waitlistFilePath)) {
            if (waitlistFilePath.getParent() != null) {
                Files.createDirectories(waitlistFilePath.getParent());
            }
            Files.createFile(waitlistFilePath);
            return loaded;
        }

        List<String> lines = Files.readAllLines(waitlistFilePath, StandardCharsets.UTF_8);

        for (String rawLine : lines) {
            if (rawLine == null || rawLine.trim().isEmpty()) {
                continue;
            }

            String[] parts = rawLine.split("\\|", -1);
            if (parts.length != 13) {
                continue;
            }

            String waitlistId = parts[0].trim();
            String patientId = parts[1].trim();
            String patientName = parts[2].trim();
            int patientAge = Integer.parseInt(parts[3].trim());
            String patientAddress = parts[4].trim();
            String doctorId = parts[5].trim();
            String doctorName = parts[6].trim();
            Department department = Department.fromText(parts[7].trim());
            LocalDate date = LocalDate.parse(parts[8].trim());
            LocalTime time = LocalTime.parse(parts[9].trim(), TIME_FORMATTER);
            WaitlistStatus status = WaitlistStatus.valueOf(parts[10].trim().toUpperCase());
            LocalDateTime createdAt = LocalDateTime.parse(parts[11].trim());
            String fulfilledAppointmentId = parts[12].trim();

            Doctor doctor = findOrCreateDoctor(doctors, doctorId, doctorName, department);
            Patient patient = new Patient(patientId, patientName, patientAge, patientAddress);

            WaitlistEntry entry = new WaitlistEntry(
                    waitlistId,
                    patient,
                    doctor,
                    date,
                    time,
                    status,
                    createdAt,
                    fulfilledAppointmentId
            );
            loaded.add(entry);
        }

        return loaded;
    }

    public java.util.Map<String, Object> loadUsers() throws IOException {
        Path usersFilePath = Paths.get(getUsersPath());

        java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
        java.util.List<com.hospital.appointment.model.UserAccount> users = new ArrayList<com.hospital.appointment.model.UserAccount>();
        java.util.Map<String, String> hashes = new java.util.HashMap<String, String>();

        if (!Files.exists(usersFilePath)) {
            if (usersFilePath.getParent() != null) {
                Files.createDirectories(usersFilePath.getParent());
            }
            Files.createFile(usersFilePath);
            result.put("users", users);
            result.put("hashes", hashes);
            return result;
        }

        java.util.List<String> lines = Files.readAllLines(usersFilePath, StandardCharsets.UTF_8);

        for (String rawLine : lines) {
            if (rawLine == null || rawLine.trim().isEmpty()) {
                continue;
            }

            String[] parts = rawLine.split("\\|");
            if (parts.length < 7) {
                continue;
            }

            try {
                String userId = parts[0].trim();
                String username = parts[1].trim();
                String passwordHash = parts[2].trim();
                String roleText = parts[3].trim();
                String linkedPatientId = parts[4].trim();
                String linkedDoctorId = parts[5].trim();
                String activeText = parts[6].trim();
                String createdAtText = parts[7].trim();

                com.hospital.appointment.enums.UserRole role = com.hospital.appointment.enums.UserRole.fromText(roleText);
                LocalDateTime createdAt = LocalDateTime.parse(createdAtText);
                boolean active = Boolean.parseBoolean(activeText);

                com.hospital.appointment.model.UserAccount account = new com.hospital.appointment.model.UserAccount(
                        userId,
                        username,
                        passwordHash,
                        role,
                        linkedPatientId,
                        linkedDoctorId,
                        createdAt
                );
                account.setActive(active);

                users.add(account);
                hashes.put(username.toLowerCase(), passwordHash);
            } catch (Exception e) {
                System.out.println("Warning: skipping malformed user line: " + e.getMessage());
            }
        }

        result.put("users", users);
        result.put("hashes", hashes);
        return result;
    }

    public void saveUsers(java.util.List<com.hospital.appointment.model.UserAccount> users, 
                         java.util.Map<String, String> hashes) throws IOException {
        Path usersFilePath = Paths.get(getUsersPath());
        java.util.List<String> lines = new ArrayList<String>();

        for (com.hospital.appointment.model.UserAccount user : users) {
            String line = String.join("|",
                    user.getUserId(),
                    user.getUsername(),
                    user.getPasswordHash(),
                    user.getRole().name(),
                    user.getLinkedPatientId(),
                    user.getLinkedDoctorId(),
                    String.valueOf(user.isActive()),
                    user.getCreatedAt().toString()
            );
            lines.add(line);
        }

        if (usersFilePath.getParent() != null) {
            Files.createDirectories(usersFilePath.getParent());
        }

        Files.write(usersFilePath, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private String getUsersPath() {
        Path appointment = appointmentFilePath;
        Path parent = appointment.getParent();
        if (parent == null) {
            return "data/users.txt";
        }
        return parent.resolve("users.txt").toString();
    }

    private static String deriveWaitlistPath(String appointmentPath) {
        Path appointment = Paths.get(appointmentPath);
        Path parent = appointment.getParent();
        if (parent == null) {
            return "waitlist.txt";
        }
        return parent.resolve("waitlist.txt").toString();
    }

    private Doctor findOrCreateDoctor(List<Doctor> doctors, String doctorId, String doctorName, Department department) {
        for (Doctor doctor : doctors) {
            if (doctor.getDoctorId().equalsIgnoreCase(doctorId)) {
                return doctor;
            }
        }

        Doctor doctor = new Doctor(doctorId, doctorName, department);
        doctors.add(doctor);
        return doctor;
    }
}