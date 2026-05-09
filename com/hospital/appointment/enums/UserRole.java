package com.hospital.appointment.enums;

/** The three roles in the system. Each role sees a different dashboard. */
public enum UserRole {
    ADMIN   ("Administrator"),
    DOCTOR  ("Doctor"),
    PATIENT ("Patient");

    private final String displayName;
    UserRole(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }

    @Override public String toString() { return displayName; }
}
