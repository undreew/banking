package org.example.banking.controllers;

import org.example.banking.constants.accounts.AccountStatus;
import org.example.banking.constants.accounts.AccountType;
import org.example.banking.dtos.account.AccountResponse;
import org.example.banking.dtos.account.CreateAccountRequest;
import org.example.banking.dtos.account.UpdateAccountStatusRequest;
import org.example.banking.dtos.transaction.DepositRequest;
import org.example.banking.dtos.transaction.TransactionResponse;
import org.example.banking.services.AccountService;
import org.example.banking.services.TransactionService;
import org.example.banking.support.WithMockActor;
import org.example.banking.support.WithRealSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@WithRealSecurity
class AccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    AccountService accountService;

    @MockitoBean
    TransactionService transactionService;

    @Test
    void createAccountWithoutTokenIsUnauthorized() throws Exception {
        var request = new CreateAccountRequest();
        request.setType(AccountType.CHECKING);

        mockMvc.perform(post("/api/v1/accounts/create")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void createAccountAsAdminIsForbidden() throws Exception {
        var request = new CreateAccountRequest();
        request.setType(AccountType.CHECKING);

        mockMvc.perform(post("/api/v1/accounts/create")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockActor(role = "USER")
    void createAccountAsUserSucceeds() throws Exception {
        var request = new CreateAccountRequest();
        request.setType(AccountType.CHECKING);

        var response = new AccountResponse(UUID.randomUUID(), "123456789012345",
                AccountType.CHECKING, BigDecimal.ZERO, AccountStatus.ACTIVE, Instant.now());
        when(accountService.createAccount(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts/create")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CHECKING"));
    }

    @Test
    @WithMockActor(role = "USER")
    void createAccountWithoutTypeIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/create")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.type").exists());
    }

    @Test
    void getAllAccountsWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockActor(role = "USER")
    void getAllAccountsAsUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void getAllAccountsAsAdminSucceeds() throws Exception {
        when(accountService.getAllAccounts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/accounts/all"))
                .andExpect(status().isOk());
    }

    @Test
    void depositWithoutTokenIsUnauthorized() throws Exception {
        var request = new DepositRequest();
        request.setAmount(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/v1/accounts/{id}/deposit", UUID.randomUUID())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void depositAsAdminIsForbidden() throws Exception {
        var request = new DepositRequest();
        request.setAmount(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/v1/accounts/{id}/deposit", UUID.randomUUID())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockActor(role = "USER")
    void depositWithoutIdempotencyKeyIsBadRequest() throws Exception {
        var request = new DepositRequest();
        request.setAmount(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/v1/accounts/{id}/deposit", UUID.randomUUID())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockActor(role = "USER")
    void depositAsOwnerSucceeds() throws Exception {
        var request = new DepositRequest();
        request.setAmount(new BigDecimal("10.00"));

        var response = new TransactionResponse();
        response.setTransactionId(UUID.randomUUID());
        when(transactionService.depositTransaction(any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts/{id}/deposit", UUID.randomUUID())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateAccountStatusWithoutTokenIsUnauthorized() throws Exception {
        var request = new UpdateAccountStatusRequest();
        request.setStatus(AccountStatus.FROZEN);
        request.setReason("Suspicious activity");

        mockMvc.perform(patch("/api/v1/accounts/{id}/status", UUID.randomUUID())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockActor(role = "USER")
    void updateAccountStatusAsUserIsForbidden() throws Exception {
        var request = new UpdateAccountStatusRequest();
        request.setStatus(AccountStatus.FROZEN);
        request.setReason("Suspicious activity");

        mockMvc.perform(patch("/api/v1/accounts/{id}/status", UUID.randomUUID())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void updateAccountStatusWithoutReasonIsBadRequest() throws Exception {
        var request = new UpdateAccountStatusRequest();
        request.setStatus(AccountStatus.FROZEN);

        mockMvc.perform(patch("/api/v1/accounts/{id}/status", UUID.randomUUID())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").exists());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void updateAccountStatusAsAdminSucceeds() throws Exception {
        var request = new UpdateAccountStatusRequest();
        request.setStatus(AccountStatus.FROZEN);
        request.setReason("Suspicious activity");

        mockMvc.perform(patch("/api/v1/accounts/{id}/status", UUID.randomUUID())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}
