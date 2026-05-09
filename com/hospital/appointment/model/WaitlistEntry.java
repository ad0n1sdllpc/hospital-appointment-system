package com.hospital.appointment.model;

import com.hospital.appointment.enums.WaitlistStatus;

/** A patient waiting for an available slot with a specific doctor on a specific date. */
public class WaitlistEntry {

    private String         waitlistId;
    private String         patientId;
    private String         doctorId;
    private String         preferredDate;
    private WaitlistStatus status;
    private String         addedAt;
    private int            queuePosition;
    private String         notes;

    // Transient resolved references
    private transient Patient patient;
    private transient Doctor  doctor;

    public WaitlistEntry(String waitlistId, String patientId, String doctorId,
                         String preferredDate, WaitlistStatus status,
                         String addedAt, int queuePosition, String notes) {
        this.waitlistId    = waitlistId;
        this.patientId     = patientId;
        this.doctorId      = doctorId;
        this.preferredDate = preferredDate;
        this.status        = status;
        this.addedAt       = addedAt;
        this.queuePosition = queuePosition;
        this.notes         = notes;
    }

    // Getters
    public String         getWaitlistId()    { return waitlistId; }
    public String         getPatientId()     { return patientId; }
    public String         getDoctorId()      { return doctorId; }
    public String         getPreferredDate() { return preferredDate; }
    public WaitlistStatus getStatus()        { return status; }
    public String         getAddedAt()       { return addedAt; }
    public int            getQueuePosition() { return queuePosition; }
    public String         getNotes()         { return notes; }
    public Patient        getPatient()       { return patient; }
    public Doctor         getDoctor()        { return doctor; }

    // Setters
    public void setStatus(WaitlistStatus v)  { this.status        = v; }
    public void setQueuePosition(int v)      { this.queuePosition = v; }
    public void setPatient(Patient v)        { this.patient       = v; }
    public void setDoctor(Doctor v)          { this.doctor        = v; }

    public void resolve(Patient p, Doctor d) { this.patient = p; this.doctor = d; }

    /** Table row for waitlist display */
    public String toTableRow() {
        String patName = (patient != null) ? truncate(patient.getName(), 18) : patientId;
        String docName = (doctor  != null) ? "Dr. " + truncate(doctor.getName(), 12) : doctorId;
        return String.format("  %-14s #%-4d %-20s %-18s %-12s %s",
            waitlistId, queuePosition, patName, docName, preferredDate, status.getDisplayName());
    }

    /** Pipe-delimited for waitlist.txt */
    public String toFileString() {
        return String.join("|",
            waitlistId, patientId, doctorId,
            preferredDate, status.name(), addedAt,
            String.valueOf(queuePosition), notes);
    }

    private String truncate(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
