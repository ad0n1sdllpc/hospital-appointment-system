package com.hospital.appointment.util;

import com.hospital.appointment.model.Appointment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdGenerator {
    private static final Pattern APPOINTMENT_PATTERN = Pattern.compile("APT-(\\d{4})-(\\d{3})");

    private final Map<Integer, Integer> appointmentCounters;
    private int patientCounter;

    public IdGenerator() {
        this.appointmentCounters = new HashMap<Integer, Integer>();
        this.patientCounter = 0;
    }

    public String nextPatientId() {
        patientCounter++;
        return String.format("PAT-%03d", patientCounter);
    }

    public String nextAppointmentId(int year) {
        int next = 1;
        if (appointmentCounters.containsKey(year)) {
            next = appointmentCounters.get(year) + 1;
        }
        appointmentCounters.put(year, next);
        return String.format("APT-%d-%03d", year, next);
    }

    public void syncFromAppointments(List<Appointment> appointments) {
        for (Appointment appointment : appointments) {
            Matcher matcher = APPOINTMENT_PATTERN.matcher(appointment.getAppointmentId());
            if (!matcher.matches()) {
                continue;
            }

            int year = Integer.parseInt(matcher.group(1));
            int sequence = Integer.parseInt(matcher.group(2));
            int current = appointmentCounters.containsKey(year) ? appointmentCounters.get(year) : 0;

            if (sequence > current) {
                appointmentCounters.put(year, sequence);
            }
        }
    }

    public void syncPatientCount(int existingPatients) {
        if (existingPatients > patientCounter) {
            patientCounter = existingPatients;
        }
    }
}