package com.hospital.appointment.storage;

import com.hospital.appointment.enums.AppointmentStatus;
import com.hospital.appointment.enums.Department;
import com.hospital.appointment.model.Appointment;
import com.hospital.appointment.model.Doctor;
import com.hospital.appointment.model.Patient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final Path filePath;

    public FileHandler(String filePath) {
        this.filePath = Paths.get(filePath);
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

        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }

        Files.write(filePath, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    public List<Appointment> loadFromFile(List<Doctor> doctors) throws IOException {
        List<Appointment> loaded = new ArrayList<Appointment>();

        if (!Files.exists(filePath)) {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.createFile(filePath);
            return loaded;
        }

        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
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