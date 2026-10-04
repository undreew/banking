package org.example.banking.controllers;

import lombok.RequiredArgsConstructor;
import org.example.banking.constants.ApiPaths;
import org.example.banking.services.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.TRANSACTIONS)
public class TransactionController {
    
    private final TransactionService transactionService;

    @GetMapping("/")
    public ResponseEntity<?> getAllTransactions() {
        transactionService.getAllTransactions();
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<?> reverseTransaction(@PathVariable String id) {
        transactionService.reverseTransaction();
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
