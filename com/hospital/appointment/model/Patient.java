package com.hospital.appointment.model;

/**
 * Domain model for a hospital patient.
 * INHERITANCE: Patient extends Person.
 * Linked to a User account via userId.
 */
public class Patient extends Person {

    private String patientId;        // PAT-YYYY-XXX
    private String userId;           // Links to User account (may be empty for walk-ins)
    private String bloodType;
    private String emergencyContact;
    private String registeredAt;

    public Patient(String patientId, String userId, String name, int age,
                   String address, String contactNumber, String email,
                   String bloodType, String emergencyContact, String registeredAt) {
        super(name, age, address, contactNumber, email);
        this.patientId       = patientId;
        this.userId          = userId;
        this.bloodType       = bloodType;
        this.emergencyContact= emergencyContact;
        this.registeredAt    = registeredAt;
    }

    // Getters
    public String getPatientId()        { return patientId; }
    public String getUserId()           { return userId; }
    public String getBloodType()        { return bloodType; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getRegisteredAt()     { return registeredAt; }

    // Setters
    public void setBloodType(String v)        { this.bloodType        = v; }
    public void setEmergencyContact(String v) { this.emergencyContact = v; }
    public void setUserId(String v)           { this.userId           = v; }

    /** Pipe-delimited for patients.txt */
    public String toFileString() {
        return String.join("|",
            patientId, userId, getName(), String.valueOf(getAge()),
            getAddress(), getContactNumber(), getEmail(),
            bloodType, emergencyContact, registeredAt);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (Age: %d)", patientId, getName(), getAge());
    }
}
