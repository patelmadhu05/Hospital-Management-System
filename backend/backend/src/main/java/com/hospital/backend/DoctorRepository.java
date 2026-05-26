package com.hospital.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    // JpaRepository gives us findall(), save(), deleteById() automatically!
    void deleteByName(String name);
}