package org.example.banking.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.banking.dtos.account.UpdateAccountStatusRequest;
import org.example.banking.dtos.transaction.DepositRequest;
import org.example.banking.dtos.transaction.TransactionResponse;
import org.example.banking.dtos.transaction.TransferRequest;
import org.example.banking.dtos.transaction.WithdrawRequest;
import org.example.banking.services.TransactionService;
import org.springframework.http.HttpStatus;
import org.example.banking.constants.ApiPaths;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.banking.services.AccountService;
import org.example.banking.dtos.account.AccountResponse;
import org.example.banking.dtos.auth.AuthenticatedActor;
import org.springframework.web.util.UriComponentsBuilder;
import org.example.banking.dtos.account.CreateAccountRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping(ApiPaths.ACCOUNTS)
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal AuthenticatedActor actor,
            UriComponentsBuilder uriBuilder) {
        var account = accountService.createAccount(request, actor.id());
        return ResponseEntity
                .created(uriBuilder.path(ApiPaths.ACCOUNTS).buildAndExpand(account.getId()).toUri())
                .body(account);
    }

    @GetMapping("/all")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        var account = accountService.getAllAccounts();
        return ResponseEntity.status(HttpStatus.OK).body(account);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountByAccountId(@PathVariable UUID id) {
        var account = accountService.getAccountByAccountId(id);
        return ResponseEntity.status(HttpStatus.OK).body(account);
    }

    @GetMapping("/")
    public ResponseEntity<List<AccountResponse>> getAllAccountsByUserId(@AuthenticationPrincipal AuthenticatedActor actor) {
        var accounts = accountService.getAllAccountsByUserId(actor.id());
        return ResponseEntity.status(HttpStatus.OK).body(accounts);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedActor actor,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request
    ) {
        var response = transactionService.depositTransaction(id, actor.id(), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedActor actor,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawRequest request
    ) {
        var response = transactionService.withdrawTransaction(id, actor.id(), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal AuthenticatedActor actor,
            @Valid @RequestBody TransferRequest request
    ) {
        var response = transactionService.transfer(actor.id(), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ADMIN
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateAccountStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        accountService.updateAccountStatus(id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
