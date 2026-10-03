package org.example.banking.mappers;

import org.example.banking.constants.accounts.AccountStatus;
import org.example.banking.constants.accounts.AccountType;
import org.example.banking.dtos.account.AccountResponse;
import org.example.banking.entities.Account;
import org.example.banking.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountResponse toView(Account account);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", source = "user")
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account toModel(User user, String accountNumber, AccountType type, AccountStatus status);
}
