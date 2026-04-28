package com.hospital.appointment.model;

import com.hospital.appointment.enums.UserRole;
import java.time.LocalDateTime;

public class UserAccount {
    private String userId;
    private String username;
    private String passwordHash;
    private UserRole role;
    private String linkedPatientId;
    private String linkedDoctorId;
    private boolean active;
    private LocalDateTime createdAt;

    public UserAccount() {
    }

    public UserAccount(String userId, String username, String passwordHash, UserRole role, 
                       String linkedPatientId, String linkedDoctorId, LocalDateTime createdAt) {
        setUserId(userId);
        setUsername(username);
        setPasswordHash(passwordHash);
        setRole(role);
        setLinkedPatientId(linkedPatientId);
        setLinkedDoctorId(linkedDoctorId);
        setActive(true);
        setCreatedAt(createdAt);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty.");
        }
        this.userId = userId.trim();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        this.username = username.trim();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be empty.");
        }
        this.passwordHash = passwordHash.trim();
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("User role cannot be null.");
        }
        this.role = role;
    }

    public String getLinkedPatientId() {
        return linkedPatientId == null ? "" : linkedPatientId;
    }

    public void setLinkedPatientId(String linkedPatientId) {
        this.linkedPatientId = linkedPatientId == null || linkedPatientId.trim().isEmpty() ? "" : linkedPatientId.trim();
    }

    public String getLinkedDoctorId() {
        return linkedDoctorId == null ? "" : linkedDoctorId;
    }

    public void setLinkedDoctorId(String linkedDoctorId) {
        this.linkedDoctorId = linkedDoctorId == null || linkedDoctorId.trim().isEmpty() ? "" : linkedDoctorId.trim();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at timestamp cannot be null.");
        }
        this.createdAt = createdAt;
    }
}
