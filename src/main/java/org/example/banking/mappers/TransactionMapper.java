package org.example.banking.mappers;

import org.example.banking.dtos.transaction.TransactionResponse;
import org.example.banking.entities.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(target = "transactionId", source = "id")
    @Mapping(target = "sourceAccountId", source = "sourceAccount.id")
    @Mapping(target = "destinationAccountId", source = "destinationAccount.id")
    TransactionResponse toView(Transaction transaction);
}
