package com.hospital.appointment.model;

/**
 * Abstract base class.  All fields are private (ENCAPSULATION).
 * Extended by Patient and User (which Doctor accounts extend via composition).
 */
public abstract class Person {

    private String name;
    private int    age;
    private String address;
    private String contactNumber;
    private String email;

    protected Person(String name, int age, String address,
                     String contactNumber, String email) {
        this.name          = name;
        this.age           = age;
        this.address       = address;
        this.contactNumber = contactNumber;
        this.email         = email;
    }

    // Getters
    public String getName()          { return name; }
    public int    getAge()           { return age; }
    public String getAddress()       { return address; }
    public String getContactNumber() { return contactNumber; }
    public String getEmail()         { return email; }

    // Setters
    public void setName(String v)          { this.name          = v; }
    public void setAge(int v)              { this.age           = v; }
    public void setAddress(String v)       { this.address       = v; }
    public void setContactNumber(String v) { this.contactNumber = v; }
    public void setEmail(String v)         { this.email         = v; }
}
