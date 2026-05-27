package com.hospital.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    // Custom deletion abstraction used dynamically by the frontend UI controller layer
    void deleteByName(String name);
}