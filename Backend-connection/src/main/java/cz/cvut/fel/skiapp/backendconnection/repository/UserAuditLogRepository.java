package cz.cvut.fel.skiapp.backendconnection.repository;

import cz.cvut.fel.skiapp.backendconnection.model.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAuditLogRepository {
    Page<UserAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<UserAuditLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);
}
