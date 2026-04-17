package com.hospital.appointment.model;

import com.hospital.appointment.enums.WaitlistStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class WaitlistEntry {
    private String waitlistId;
    private Patient patient;
    private Doctor doctor;
    private LocalDate date;
    private LocalTime time;
    private WaitlistStatus status;
    private LocalDateTime createdAt;
    private String fulfilledAppointmentId;

    public WaitlistEntry() {
    }

    public WaitlistEntry(String waitlistId,
                         Patient patient,
                         Doctor doctor,
                         LocalDate date,
                         LocalTime time,
                         WaitlistStatus status,
                         LocalDateTime createdAt,
                         String fulfilledAppointmentId) {
        setWaitlistId(waitlistId);
        setPatient(patient);
        setDoctor(doctor);
        setDate(date);
        setTime(time);
        setStatus(status);
        setCreatedAt(createdAt);
        setFulfilledAppointmentId(fulfilledAppointmentId);
    }

    public String getWaitlistId() {
        return waitlistId;
    }

    public void setWaitlistId(String waitlistId) {
        if (waitlistId == null || waitlistId.trim().isEmpty()) {
            throw new IllegalArgumentException("Waitlist ID cannot be empty.");
        }
        this.waitlistId = waitlistId.trim();
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient cannot be null.");
        }
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        if (doctor == null) {
            throw new IllegalArgumentException("Doctor cannot be null.");
        }
        this.doctor = doctor;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        if (time == null) {
            throw new IllegalArgumentException("Time cannot be null.");
        }
        this.time = time;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null.");
        }
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Created date-time cannot be null.");
        }
        this.createdAt = createdAt;
    }

    public String getFulfilledAppointmentId() {
        return fulfilledAppointmentId;
    }

    public void setFulfilledAppointmentId(String fulfilledAppointmentId) {
        if (fulfilledAppointmentId == null || fulfilledAppointmentId.trim().isEmpty()) {
            this.fulfilledAppointmentId = "";
            return;
        }
        this.fulfilledAppointmentId = fulfilledAppointmentId.trim();
    }
}
