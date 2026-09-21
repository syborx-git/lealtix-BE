package com.lealtixservice.repository;

import com.lealtixservice.entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, Long> {
    Optional<Allergy> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}