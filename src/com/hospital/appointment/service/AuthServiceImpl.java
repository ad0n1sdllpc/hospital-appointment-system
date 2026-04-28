package com.hospital.appointment.service;

import com.hospital.appointment.enums.UserRole;
import com.hospital.appointment.model.SessionContext;
import com.hospital.appointment.model.UserAccount;
import com.hospital.appointment.storage.FileHandler;
import com.hospital.appointment.util.InputValidator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuthServiceImpl implements AuthService {
    private final FileHandler fileHandler;
    private final Map<String, UserAccount> usersCache;
    private final Map<String, String> passwordHashCache;

    public AuthServiceImpl(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
        this.usersCache = new HashMap<String, UserAccount>();
        this.passwordHashCache = new HashMap<String, String>();
        loadUsers();
    }

    @Override
    public Optional<SessionContext> login(String username, String password) {
        InputValidator.requireNonBlank(username, "Username");
        InputValidator.requireNonBlank(password, "Password");

        UserAccount account = usersCache.get(username.trim().toLowerCase());
        if (account == null || !account.isActive()) {
            return Optional.empty();
        }

        String storedHash = passwordHashCache.get(username.trim().toLowerCase());
        if (storedHash == null || !verifyPassword(password, storedHash)) {
            return Optional.empty();
        }

        SessionContext session = new SessionContext(account, LocalDateTime.now());
        return Optional.of(session);
    }

    @Override
    public boolean verifyPassword(String plaintext, String hash) {
        if (plaintext == null || hash == null) {
            return false;
        }
        
        // Simple PBKDF2-style verification: for demo, use hashCode
        // In production, use javax.crypto or Spring Security
        return hashPassword(plaintext).equals(hash);
    }

    @Override
    public String hashPassword(String plaintext) {
        if (plaintext == null) {
            return "";
        }
        
        // Simple hash for demo: in production use PBKDF2 or bcrypt
        // Format: hash value as hex
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password: " + e.getMessage());
        }
    }

    @Override
    public UserAccount registerUser(String username, String plaintextPassword, String role, 
                                   String linkedPatientId, String linkedDoctorId) {
        InputValidator.requireNonBlank(username, "Username");
        InputValidator.requireNonBlank(plaintextPassword, "Password");
        InputValidator.requireNonBlank(role, "Role");

        String usernameLower = username.trim().toLowerCase();
        if (usersCache.containsKey(usernameLower)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        UserRole userRole = UserRole.fromText(role);
        String userId = generateUserId(userRole);
        String passwordHash = hashPassword(plaintextPassword);

        UserAccount account = new UserAccount(
                userId,
                username,
                passwordHash,
                userRole,
                linkedPatientId == null ? "" : linkedPatientId.trim(),
                linkedDoctorId == null ? "" : linkedDoctorId.trim(),
                LocalDateTime.now()
        );

        usersCache.put(usernameLower, account);
        passwordHashCache.put(usernameLower, passwordHash);
        
        try {
            fileHandler.saveUsers(new ArrayList<UserAccount>(usersCache.values()), 
                                 new HashMap<String, String>(passwordHashCache));
        } catch (Exception e) {
            // Rollback on save failure
            usersCache.remove(usernameLower);
            passwordHashCache.remove(usernameLower);
            throw new RuntimeException("Failed to register user: " + e.getMessage());
        }

        return account;
    }

    @Override
    public boolean userExists(String username) {
        if (username == null) {
            return false;
        }
        return usersCache.containsKey(username.trim().toLowerCase());
    }

    @Override
    public List<UserAccount> getAllUsers() {
        return new ArrayList<UserAccount>(usersCache.values());
    }

    private void loadUsers() {
        try {
            Map<String, Object> loadedData = fileHandler.loadUsers();
            @SuppressWarnings("unchecked")
            List<UserAccount> accounts = (List<UserAccount>) loadedData.get("users");
            @SuppressWarnings("unchecked")
            Map<String, String> hashes = (Map<String, String>) loadedData.get("hashes");

            for (UserAccount account : accounts) {
                usersCache.put(account.getUsername().toLowerCase(), account);
            }
            passwordHashCache.putAll(hashes);
        } catch (Exception e) {
            System.out.println("Could not load existing users: " + e.getMessage());
        }
    }

    private String generateUserId(UserRole role) {
        int count = 0;
        for (UserAccount account : usersCache.values()) {
            if (account.getRole() == role) {
                count++;
            }
        }
        return role.name().substring(0, 1) + "-" + String.format("%03d", count + 1);
    }
}
