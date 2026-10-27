package org.example.banking.services;

import lombok.RequiredArgsConstructor;
import org.example.banking.constants.audit.AuditAction;
import org.example.banking.entities.AuditLog;
import org.example.banking.repositories.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes security events. Always commits independently of the caller so an event survives a
 * calling transaction that ends in an exception (failed login, reuse detection).
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID performedBy,
            AuditAction action,
            String targetEntity,
            UUID targetId,
            String details,
            String ipAddress
    ) {
        auditLogRepository.save(
                AuditLog.record(performedBy, action, targetEntity, targetId, details, ipAddress)
        );
    }
}