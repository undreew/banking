package org.example.banking.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.banking.constants.ApiPaths;
import org.example.banking.dtos.account.AccountResponse;
import org.example.banking.dtos.account.UpdateAccountStatusRequest;
import org.example.banking.services.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Back-office account operations. The URL prefix is additionally guarded by SecurityConfig.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.ADMIN_ACCOUNTS)
public class AdminAccountController {

    private final AccountService accountService;

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountByAccountId(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and @access.sessionLive(principal)")
    public ResponseEntity<Void> updateAccountStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        accountService.updateAccountStatus(id, request);
        return ResponseEntity.noContent().build();
    }
}