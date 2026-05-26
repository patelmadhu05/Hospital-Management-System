package com.hospital.backend;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctors") // Maps this class to the 'doctors' table in MySQL
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Handles auto-incrementing IDs
    private Long id;

    private String name;
    private String specialization;
    private String contact;       // Added to track contact details
    private String availability;  // Added to track availability shifts

    // Default Constructor (Required by JPA)
    public Doctor() {}

    // Constructor with fields
    public Doctor(String name, String specialization, String contact, String availability) {
        this.name = name;
        this.specialization = specialization;
        this.contact = contact;
        this.availability = availability;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
}