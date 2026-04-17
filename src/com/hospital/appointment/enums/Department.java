package com.hospital.appointment.enums;

public enum Department {
    CARDIOLOGY,
    PEDIATRICS,
    ORTHOPEDICS,
    DERMATOLOGY,
    NEUROLOGY,
    GENERAL_MEDICINE;

    public static Department fromText(String value) {
        return Department.valueOf(value.trim().toUpperCase());
    }
}