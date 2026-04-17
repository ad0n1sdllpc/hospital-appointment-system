package com.hospital.appointment.service;

import com.hospital.appointment.model.Appointment;
import com.hospital.appointment.model.Doctor;
import com.hospital.appointment.model.Patient;
import com.hospital.appointment.model.WaitlistEntry;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface AppointmentService {
    Appointment addAppointment(Patient patient, String doctorId, LocalDate date, LocalTime time);

    List<Appointment> viewAppointments();

    boolean cancelAppointment(String appointmentId);

    List<Appointment> searchAppointment(String patientName, LocalDate date, String doctorName);

    Appointment rescheduleAppointment(String appointmentId, LocalDate newDate, LocalTime newTime);

    boolean completeAppointment(String appointmentId);

    List<Appointment> getPatientHistory(String patientName);

    List<Appointment> getDoctorDailySchedule(String doctorId, LocalDate date);

    WaitlistEntry addToWaitlist(Patient patient, String doctorId, LocalDate date, LocalTime time);

    List<WaitlistEntry> viewWaitlist();

    boolean cancelWaitlistEntry(String waitlistId);

    Map<String, Integer> getReportSummary();

    List<Doctor> getDoctors();

    List<LocalTime> getAvailableTimeSlots(String doctorId, LocalDate date);

    List<LocalTime> getTimeSlots();
}