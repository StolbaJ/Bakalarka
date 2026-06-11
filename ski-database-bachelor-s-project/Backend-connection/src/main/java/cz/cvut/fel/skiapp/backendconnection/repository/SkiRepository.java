package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.Ski;
import cz.cvut.fel.skiapp.backendconnection.model.SkiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkiRepository extends JpaRepository<Ski, Long> {
    Optional<Ski> findBySkiNumber(String skiNumber);
    List<Ski> findByStatus(SkiStatus status);
    boolean existsBySkiNumber(String skiNumber);
}
