package com.hospital.appointment.enums;

public enum UserRole {
    ADMIN("Admin"),
    PATIENT("Patient"),
    DOCTOR("Doctor");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UserRole fromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("User role cannot be empty.");
        }

        for (UserRole role : UserRole.values()) {
            if (role.name().equalsIgnoreCase(text.trim())) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown user role: " + text);
    }
}
