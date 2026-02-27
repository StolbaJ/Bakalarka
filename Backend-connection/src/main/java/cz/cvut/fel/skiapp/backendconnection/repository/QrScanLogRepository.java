package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.QrScanLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface QrScanLogRepository extends JpaRepository<QrScanLog, Long> {
    @EntityGraph(attributePaths = {"ski"})
    List<QrScanLog> findByUserIdOrderByScannedAtDesc(Long userId, Pageable pageable);

    void deleteByUserIdAndIdNotIn(Long userId, Set<Long> idsToKeep);
}
