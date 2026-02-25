package com.ski.inventory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Záznam auditu změn uživatelských účtů.
 * Ukládá kdo, kdy a jakou změnu provedl.
 */
@Entity
@Table(name = "user_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "user_audit_action")
    private UserAuditAction action;

    /** ID uživatele, kterého se změna týká */
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    /** Username cílového uživatele */
    @Column(name = "target_username", nullable = false, length = 50)
    private String targetUsername;

    /** Username administrátora, který změnu provedl */
    @Column(name = "performed_by", nullable = false, length = 50)
    private String performedBy;

    /** Detaily změny (např. stará/nová role, email pro reset hesla) */
    @Column(name = "details", length = 500)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
