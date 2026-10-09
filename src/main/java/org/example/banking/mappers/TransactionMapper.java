package org.example.banking.mappers;

import org.example.banking.dtos.transaction.TransactionResponse;
import org.example.banking.entities.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(target = "transactionId", source = "transaction.id")
    TransactionResponse toView(Transaction transaction);

    @Mapping(target = "transactionId", source = "transaction.id")
    TransactionResponse transactToView(Transaction transaction, BigDecimal amount);
}
