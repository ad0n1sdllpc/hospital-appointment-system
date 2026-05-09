package com.hospital.appointment.model;

import com.hospital.appointment.enums.AppointmentStatus;

/** Represents one hospital appointment linking a Patient and a Doctor. */
public class Appointment {

    private String            appointmentId;  // APT-YYYY-XXX
    private String            patientId;      // FK to Patient
    private String            doctorId;       // FK to Doctor
    private String            date;           // yyyy-MM-dd
    private String            timeSlot;       // HH:MM
    private AppointmentStatus status;
    private String            notes;
    private String            createdAt;

    // Runtime references (not stored separately; resolved after load)
    private transient Patient patient;
    private transient Doctor  doctor;

    public Appointment(String appointmentId, String patientId, String doctorId,
                       String date, String timeSlot, AppointmentStatus status,
                       String notes, String createdAt) {
        this.appointmentId = appointmentId;
        this.patientId     = patientId;
        this.doctorId      = doctorId;
        this.date          = date;
        this.timeSlot      = timeSlot;
        this.status        = status;
        this.notes         = notes;
        this.createdAt     = createdAt;
    }

    // Key Getters
    public String            getAppointmentId() { return appointmentId; }
    public String            getPatientId()     { return patientId; }
    public String            getDoctorId()      { return doctorId; }
    public String            getDate()          { return date; }
    public String            getTimeSlot()      { return timeSlot; }
    public AppointmentStatus getStatus()        { return status; }
    public String            getNotes()         { return notes; }
    public String            getCreatedAt()     { return createdAt; }
    public Patient           getPatient()       { return patient; }
    public Doctor            getDoctor()        { return doctor; }

    // Setters
    public void setStatus(AppointmentStatus v)   { this.status   = v; }
    public void setDate(String v)                { this.date     = v; }
    public void setTimeSlot(String v)            { this.timeSlot = v; }
    public void setDoctorId(String v)            { this.doctorId = v; }
    public void setNotes(String v)               { this.notes    = v; }
    public void setPatient(Patient v)            { this.patient  = v; }
    public void setDoctor(Doctor v)              { this.doctor   = v; }

    /** Called after loading to hydrate transient references. */
    public void resolve(Patient p, Doctor d) {
        this.patient = p;
        this.doctor  = d;
    }

    /** Compact table row for list views. */
    public String toTableRow() {
        String patName  = (patient != null) ? truncate(patient.getName(), 18)    : patientId;
        String docName  = (doctor  != null) ? "Dr. " + truncate(doctor.getName(), 14) : doctorId;
        String dept     = (doctor  != null) ? truncate(doctor.getDepartment().getDisplayName(), 18) : "---";
        return String.format("  %-16s %-20s %-20s %-18s %-12s %-6s %-11s",
            appointmentId, patName, docName, dept, date, timeSlot, status.getDisplayName());
    }

    /** Full detail block for single-record view. */
    public String toDetailBlock() {
        String line = "=".repeat(64);
        String patName    = (patient != null) ? patient.getName()              : patientId;
        String patAge     = (patient != null) ? String.valueOf(patient.getAge()) : "?";
        String patAddr    = (patient != null) ? patient.getAddress()           : "?";
        String patContact = (patient != null) ? patient.getContactNumber()     : "?";
        String patBlood   = (patient != null) ? patient.getBloodType()         : "?";
        String docName    = (doctor  != null) ? "Dr. " + doctor.getName()      : doctorId;
        String dept       = (doctor  != null) ? doctor.getDepartment().getDisplayName() : "?";
        String spec       = (doctor  != null) ? doctor.getSpecialization()     : "?";

        return String.format(
            "%n  %s%n" +
            "  Appointment ID  : %s%n" +
            "  Status          : %s%n" +
            "  --- PATIENT ----%n" +
            "  Patient ID      : %s%n" +
            "  Name            : %s%n" +
            "  Age             : %s%n" +
            "  Address         : %s%n" +
            "  Contact         : %s%n" +
            "  Blood Type      : %s%n" +
            "  --- APPOINTMENT ----%n" +
            "  Doctor          : %s%n" +
            "  Department      : %s%n" +
            "  Specialization  : %s%n" +
            "  Date            : %s%n" +
            "  Time            : %s%n" +
            "  Notes           : %s%n" +
            "  Booked On       : %s%n" +
            "  %s%n",
            line,
            appointmentId,
            status.getLabel(),
            patientId, patName, patAge, patAddr, patContact,
            patBlood.isEmpty() ? "N/A" : patBlood,
            docName, dept, spec,
            date, timeSlot,
            notes.isEmpty() ? "None" : notes,
            createdAt,
            line
        );
    }

    /** Pipe-delimited for appointments.txt */
    public String toFileString() {
        return String.join("|",
            appointmentId, patientId, doctorId,
            date, timeSlot, status.name(), notes, createdAt);
    }

    private String truncate(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
