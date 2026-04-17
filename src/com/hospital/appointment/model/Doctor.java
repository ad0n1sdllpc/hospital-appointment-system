package com.hospital.appointment.model;

import com.hospital.appointment.enums.Department;

public class Doctor {
    private String doctorId;
    private String name;
    private Department department;

    public Doctor() {
    }

    public Doctor(String doctorId, String name, Department department) {
        setDoctorId(doctorId);
        setName(name);
        setDepartment(department);
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) {
            throw new IllegalArgumentException("Doctor ID cannot be empty.");
        }
        this.doctorId = doctorId.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Doctor name cannot be empty.");
        }
        this.name = name.trim();
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        if (department == null) {
            throw new IllegalArgumentException("Department cannot be null.");
        }
        this.department = department;
    }
}