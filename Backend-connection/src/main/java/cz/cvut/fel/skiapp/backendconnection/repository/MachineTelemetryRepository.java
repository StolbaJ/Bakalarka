package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.MachineTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineTelemetryRepository extends JpaRepository<MachineTelemetry, Long> {
}
