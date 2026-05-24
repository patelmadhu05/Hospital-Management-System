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

    // Default Constructor (Required by JPA)
    public Doctor() {}

    // Constructor with fields
    public Doctor(String name, String specialization) {
        this.name = name;
        this.specialization = specialization;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
}