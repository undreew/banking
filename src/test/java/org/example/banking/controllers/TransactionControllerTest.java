package org.example.banking.controllers;

import org.example.banking.dtos.transaction.ReverseTransactionRequest;
import org.example.banking.dtos.transaction.TransactionResponse;
import org.example.banking.services.TransactionService;
import org.example.banking.support.WithMockActor;
import org.example.banking.support.WithRealSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@WithRealSecurity
class TransactionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    TransactionService transactionService;

    @Test
    void getAllTransactionsWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/"))
                .andExpect(status().isUnauthorized());
    }

    // Role and AccountStatus have no hierarchy (spec Section 2): an admin hitting a
    // hasRole('USER') endpoint gets 403, not an implicit pass.
    @Test
    @WithMockActor(role = "ADMIN")
    void getAllTransactionsAsAdminIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockActor(role = "USER")
    void getAllTransactionsAsUserSucceeds() throws Exception {
        when(transactionService.getAllTransactions(any(), any(), any()))
                .thenReturn(Page.empty(Pageable.unpaged()));

        mockMvc.perform(get("/api/v1/transactions/"))
                .andExpect(status().isOk());
    }

    @Test
    void reverseTransactionWithoutTokenIsUnauthorized() throws Exception {
        var request = new ReverseTransactionRequest();
        request.setReason("Customer dispute");

        mockMvc.perform(post("/api/v1/transactions/{id}/reverse", UUID.randomUUID())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockActor(role = "USER")
    void reverseTransactionAsUserIsForbidden() throws Exception {
        var request = new ReverseTransactionRequest();
        request.setReason("Customer dispute");

        mockMvc.perform(post("/api/v1/transactions/{id}/reverse", UUID.randomUUID())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void reverseTransactionWithoutReasonIsBadRequest() throws Exception {
        var request = new ReverseTransactionRequest();

        mockMvc.perform(post("/api/v1/transactions/{id}/reverse", UUID.randomUUID())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").exists());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void reverseTransactionAsAdminSucceeds() throws Exception {
        var request = new ReverseTransactionRequest();
        request.setReason("Customer dispute");

        var response = new TransactionResponse();
        response.setTransactionId(UUID.randomUUID());
        when(transactionService.reverseTransaction(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions/{id}/reverse", UUID.randomUUID())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
