package com.hospital.appointment.model;

import com.hospital.appointment.enums.UserRole;

/**
 * Represents a system login account (Admin, Doctor, or Patient user).
 *
 * Separation of concerns:
 *   - User  = authentication/authorization identity
 *   - Patient / Doctor = domain data linked to a User via userId
 *
 * Passwords are stored as plain text here (sufficient for a console/thesis project).
 * In production, you would hash with BCrypt.
 */
public class User {

    private String   userId;       // USR-001
    private String   username;
    private String   password;
    private UserRole role;
    private String   linkedId;     // patientId or doctorId, empty for ADMIN
    private String   fullName;
    private boolean  active;
    private String   createdAt;

    public User(String userId, String username, String password,
                UserRole role, String linkedId, String fullName,
                boolean active, String createdAt) {
        this.userId    = userId;
        this.username  = username;
        this.password  = password;
        this.role      = role;
        this.linkedId  = linkedId;
        this.fullName  = fullName;
        this.active    = active;
        this.createdAt = createdAt;
    }

    // Getters
    public String   getUserId()   { return userId; }
    public String   getUsername() { return username; }
    public String   getPassword() { return password; }
    public UserRole getRole()     { return role; }
    public String   getLinkedId() { return linkedId; }
    public String   getFullName() { return fullName; }
    public boolean  isActive()    { return active; }
    public String   getCreatedAt(){ return createdAt; }

    // Setters
    public void setPassword(String v) { this.password = v; }
    public void setLinkedId(String v) { this.linkedId = v; }
    public void setFullName(String v) { this.fullName = v; }
    public void setActive(boolean v)  { this.active   = v; }

    /** Pipe-delimited string for users.txt */
    public String toFileString() {
        return String.join("|",
            userId, username, password, role.name(),
            linkedId, fullName, String.valueOf(active), createdAt);
    }

    /** Check password */
    public boolean checkPassword(String input) {
        return this.password.equals(input);
    }
}
