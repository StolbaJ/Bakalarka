package com.ski.inventory.repository;

import com.ski.inventory.model.Ski;
import com.ski.inventory.model.SkiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface SkiRepository extends JpaRepository<Ski, Long> {
    Optional<Ski> findBySkiNumber(String skiNumber);
    List<Ski> findByStatus(SkiStatus status);
    boolean existsBySkiNumber(String skiNumber);
}
