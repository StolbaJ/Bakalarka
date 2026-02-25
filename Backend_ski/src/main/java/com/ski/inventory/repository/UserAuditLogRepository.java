package com.ski.inventory.repository;

import com.ski.inventory.model.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    Page<UserAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<UserAuditLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);
}
