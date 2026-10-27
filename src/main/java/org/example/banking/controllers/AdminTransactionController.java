package org.example.banking.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.banking.constants.ApiPaths;
import org.example.banking.dtos.transaction.ReverseTransactionRequest;
import org.example.banking.dtos.transaction.TransactionResponse;
import org.example.banking.services.TransactionService;
import org.example.banking.validators.WithRateLimit;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.ADMIN_TRANSACTIONS)
public class AdminTransactionController {

    private final TransactionService transactionService;

    @WithRateLimit
    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN') and @access.sessionLive(principal)")
    public ResponseEntity<TransactionResponse> reverseTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody ReverseTransactionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return ResponseEntity.ok(
                transactionService.reverseTransaction(id, idempotencyKey, request)
        );
    }
}