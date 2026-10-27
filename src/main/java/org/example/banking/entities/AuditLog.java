package org.example.banking.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.example.banking.constants.audit.AuditAction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "performed_by", nullable = false, updatable = false)
    private UUID performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50, updatable = false)
    private AuditAction action;

    @Column(name = "target_entity", nullable = false, length = 50, updatable = false)
    private String targetEntity;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Column(name = "details", columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public static AuditLog record(
            UUID performedBy,
            AuditAction action,
            String targetEntity,
            UUID targetId,
            String details,
            String ipAddress
    ) {
        AuditLog log = new AuditLog();
        log.id = UUID.randomUUID();
        log.performedBy = Objects.requireNonNull(performedBy, "performedBy");
        log.action = Objects.requireNonNull(action, "action");
        log.targetEntity = Objects.requireNonNull(targetEntity, "targetEntity");
        log.targetId = Objects.requireNonNull(targetId, "targetId");
        log.details = details;
        log.ipAddress = ipAddress;

        return log;
    }
}