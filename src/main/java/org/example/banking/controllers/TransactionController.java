package org.example.banking.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.banking.constants.ApiPaths;
import org.example.banking.constants.transactions.TransactionStatus;
import org.example.banking.constants.transactions.TransactionType;
import org.example.banking.dtos.PageResponse;
import org.example.banking.dtos.auth.AuthenticatedActor;
import org.example.banking.dtos.transaction.TransactionFilter;
import org.example.banking.dtos.transaction.TransactionResponse;
import org.example.banking.dtos.transaction.ReverseTransactionRequest;
import org.example.banking.services.TransactionService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.TRANSACTIONS)
public class TransactionController {
    
    private final TransactionService transactionService;

    @GetMapping("/")
    public ResponseEntity<PageResponse<TransactionResponse>> getAllTransactions(
            @AuthenticationPrincipal AuthenticatedActor actor,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            Pageable pageable
    ) {
        var filter = new TransactionFilter(accountId, type, status, dateFrom, dateTo);
        var transactions = transactionService.getAllTransactions(filter, actor, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.from(transactions));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<TransactionResponse> reverseTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody ReverseTransactionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
            ) {
        var response = transactionService.reverseTransaction(id, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
