package com.hospital.appointment.service;

import com.hospital.appointment.model.SessionContext;
import com.hospital.appointment.model.UserAccount;
import java.util.Optional;

public interface AuthService {
    /**
     * Authenticate a user with username and password.
     * @param username the username
     * @param password the plaintext password
     * @return Optional containing SessionContext if authentication succeeds
     */
    Optional<SessionContext> login(String username, String password);

    /**
     * Verify if a plaintext password matches the stored hash.
     * @param plaintext the plaintext password
     * @param hash the stored password hash
     * @return true if password matches, false otherwise
     */
    boolean verifyPassword(String plaintext, String hash);

    /**
     * Hash a plaintext password for storage.
     * @param plaintext the plaintext password
     * @return the hashed password
     */
    String hashPassword(String plaintext);

    /**
     * Register a new user account.
     * @param username the username
     * @param plaintextPassword the plaintext password
     * @param role the user role
     * @param linkedPatientId the linked patient ID (for patient role)
     * @param linkedDoctorId the linked doctor ID (for doctor role)
     * @return the created UserAccount
     * @throws IllegalArgumentException if username already exists
     */
    UserAccount registerUser(String username, String plaintextPassword, String role, 
                            String linkedPatientId, String linkedDoctorId);

    /**
     * Check if a username is already registered.
     * @param username the username to check
     * @return true if username exists
     */
    boolean userExists(String username);

    /**
     * Get all registered users (for admin/maintenance only).
     * @return list of UserAccount objects
     */
    java.util.List<UserAccount> getAllUsers();
}
