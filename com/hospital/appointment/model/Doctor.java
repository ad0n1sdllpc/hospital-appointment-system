package com.hospital.appointment.model;

import com.hospital.appointment.enums.Department;

/**
 * Domain model for a hospital doctor.
 * Linked to a User account via userId for login.
 */
public class Doctor {

    public static final String EMPTY_DATE_SLOTS = "DATES:";

    private String     doctorId;          // D-01
    private String     userId;            // Links to User account
    private String     name;
    private Department department;
    private String     specialization;
    private int        yearsOfExperience;
    private String     schedule;          // legacy field; doctors are 24/7 by default
    private String     availableSlots;    // date-specific slots only: DATES:yyyy-MM-dd=HH:MM,...

    public Doctor(String doctorId, String userId, String name, Department department,
                  String specialization, int yearsOfExperience,
                  String schedule, String availableSlots) {
        this.doctorId          = doctorId;
        this.userId            = userId;
        this.name              = name;
        this.department        = department;
        this.specialization    = specialization;
        this.yearsOfExperience = yearsOfExperience;
        this.schedule          = schedule;
        this.availableSlots    = availableSlots;
    }

    // Getters
    public String     getDoctorId()          { return doctorId; }
    public String     getUserId()            { return userId; }
    public String     getName()              { return name; }
    public Department getDepartment()        { return department; }
    public String     getSpecialization()    { return specialization; }
    public int        getYearsOfExperience() { return yearsOfExperience; }
    public String     getSchedule()          { return schedule; }
    public String     getAvailableSlots()    { return availableSlots; }

    // Setters
    public void setSchedule(String v)       { this.schedule       = v; }
    public void setAvailableSlots(String v) { this.availableSlots = v; }
    public void setSpecialization(String v) { this.specialization = v; }
    public void setUserId(String v)         { this.userId         = v; }

    /** Pipe-delimited for doctors.txt */
    public String toFileString() {
        return String.join("|",
            doctorId, userId, name, department.name(),
            specialization, String.valueOf(yearsOfExperience),
            schedule, availableSlots);
    }

    /** Row used in doctor selection lists */
    public String toListEntry(int index) {
        return String.format("  %2d | %-5s | %-20s | %-26s | %d yrs",
            index, doctorId, name, department.getDisplayName(), yearsOfExperience);
    }

    @Override
    public String toString() {
        return String.format("[%s] Dr. %-18s | %s", doctorId, name, department.getDisplayName());
    }
}
