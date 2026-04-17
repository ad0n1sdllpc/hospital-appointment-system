package com.hospital.appointment.model;

public class Patient extends Person {
    private String patientId;

    public Patient() {
        super();
    }

    public Patient(String patientId, String name, int age, String address) {
        super(name, age, address);
        setPatientId(patientId);
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        if (patientId == null || patientId.trim().isEmpty()) {
            this.patientId = "";
            return;
        }
        this.patientId = patientId.trim();
    }
}