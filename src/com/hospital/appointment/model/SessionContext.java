package com.hospital.appointment.model;

import com.hospital.appointment.enums.UserRole;
import java.time.LocalDateTime;

public class SessionContext {
    private UserAccount currentUser;
    private LocalDateTime loginTime;

    public SessionContext(UserAccount currentUser, LocalDateTime loginTime) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user cannot be null.");
        }
        if (loginTime == null) {
            throw new IllegalArgumentException("Login time cannot be null.");
        }
        this.currentUser = currentUser;
        this.loginTime = loginTime;
    }

    public UserAccount getCurrentUser() {
        return currentUser;
    }

    public String getUserId() {
        return currentUser.getUserId();
    }

    public String getUsername() {
        return currentUser.getUsername();
    }

    public UserRole getRole() {
        return currentUser.getRole();
    }

    public String getLinkedPatientId() {
        return currentUser.getLinkedPatientId();
    }

    public String getLinkedDoctorId() {
        return currentUser.getLinkedDoctorId();
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public boolean isPatient() {
        return currentUser.getRole() == UserRole.PATIENT;
    }

    public boolean isDoctor() {
        return currentUser.getRole() == UserRole.DOCTOR;
    }

    public boolean isAdmin() {
        return currentUser.getRole() == UserRole.ADMIN;
    }

    public boolean hasRole(UserRole... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        for (UserRole role : roles) {
            if (currentUser.getRole() == role) {
                return true;
            }
        }
        return false;
    }
}
