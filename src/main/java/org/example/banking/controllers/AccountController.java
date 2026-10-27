package org.example.banking.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.banking.constants.ApiPaths;
import org.example.banking.dtos.account.AccountResponse;
import org.example.banking.dtos.account.CreateAccountRequest;
import org.example.banking.dtos.auth.AuthenticatedActor;
import org.example.banking.dtos.transaction.DepositRequest;
import org.example.banking.dtos.transaction.TransactionResponse;
import org.example.banking.dtos.transaction.TransferRequest;
import org.example.banking.dtos.transaction.WithdrawRequest;
import org.example.banking.services.AccountService;
import org.example.banking.services.TransactionService;
import org.example.banking.validators.WithRateLimit;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

/**
 * Customer-facing account operations. Ownership is decided by {@code AccessPolicy} in the
 * annotations; money-moving operations additionally require a live session.
 * Administrative operations live in {@link AdminAccountController}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.ACCOUNTS)
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal AuthenticatedActor actor,
            UriComponentsBuilder uriBuilder
    ) {
        var account = accountService.createAccount(request, actor.id());

        return ResponseEntity
                .created(
                        uriBuilder.path(ApiPaths.ACCOUNTS + "/{id}")
                                .buildAndExpand(account.getId())
                                .toUri()
                )
                .body(account);
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AccountResponse>> getAllAccountsByUserId(
            @AuthenticationPrincipal AuthenticatedActor actor
    ) {
        return ResponseEntity.ok(accountService.getAllAccountsByUserId(actor.id()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.ownsAccount(#id, principal)")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountByAccountId(id));
    }

    @WithRateLimit
    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasRole('USER') and @access.ownsAccount(#id, principal) and @access.sessionLive(principal)")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedActor actor,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request
    ) {
        return ResponseEntity.ok(
                transactionService.depositTransaction(id, actor.id(), idempotencyKey, request)
        );
    }

    @WithRateLimit
    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('USER') and @access.ownsAccount(#id, principal) and @access.sessionLive(principal)")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedActor actor,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawRequest request
    ) {
        return ResponseEntity.ok(
                transactionService.withdrawTransaction(id, actor.id(), idempotencyKey, request)
        );
    }

    @WithRateLimit
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER') and @access.ownsAccount(#request.sourceAccountId, principal) and @access.sessionLive(principal)")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal AuthenticatedActor actor,
            @Valid @RequestBody TransferRequest request
    ) {
        return ResponseEntity.ok(
                transactionService.transfer(actor.id(), idempotencyKey, request)
        );
    }
}