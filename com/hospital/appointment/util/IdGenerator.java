package com.hospital.appointment.util;

import java.time.LocalDate;

/** Generates and syncs all entity IDs. */
public class IdGenerator {

    private int appointmentSeq = 1;
    private int patientSeq     = 1;
    private int userSeq        = 1;

    public String nextAppointmentId() {
        return String.format("APT-%d-%03d", LocalDate.now().getYear(), appointmentSeq++);
    }

    public String nextPatientId() {
        return String.format("PAT-%d-%03d", LocalDate.now().getYear(), patientSeq++);
    }

    public String nextUserId() {
        return String.format("USR-%03d", userSeq++);
    }

    public void syncAppointment(int n) { if (n >= appointmentSeq) appointmentSeq = n + 1; }
    public void syncPatient(int n)     { if (n >= patientSeq)     patientSeq     = n + 1; }
    public void syncUser(int n)        { if (n >= userSeq)        userSeq        = n + 1; }

    /** Extract trailing numeric part of an ID like APT-2026-007 or USR-003. */
    public static int seq(String id) {
        if (id == null || id.isEmpty()) return 0;
        String[] parts = id.split("-");
        try { return Integer.parseInt(parts[parts.length - 1]); }
        catch (NumberFormatException e) { return 0; }
    }
}
