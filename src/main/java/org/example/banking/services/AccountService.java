package org.example.banking.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.banking.constants.accounts.AccountStatus;
import org.example.banking.dtos.account.AccountResponse;
import org.example.banking.dtos.account.CreateAccountRequest;
import org.example.banking.dtos.account.UpdateAccountStatusRequest;
import org.example.banking.exceptions.ConflictException;
import org.example.banking.exceptions.NotFoundException;
import org.example.banking.mappers.AccountMapper;
import org.example.banking.repositories.AccountRepository;
import org.example.banking.repositories.UserRepository;
import org.example.banking.utils.AccountNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final AccountMapper accountMapper;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public AccountResponse createAccount(CreateAccountRequest request, UUID userId) {
        var user = userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("User", userId));
        var account = accountRepository.save(accountMapper.toModel(user, AccountNumberGenerator.generateAccountNumber(), request.getType(), AccountStatus.ACTIVE));
        logger.info("account created id={} account_number={}", account.getId(), account.getAccountNumber());
        return accountMapper.toView(account);
    }

    public List<AccountResponse> getAllAccounts() {
        var accounts = accountRepository.findAll();
        return accounts.stream().map(accountMapper::toView).collect(Collectors.toList());
    }

    public AccountResponse getAccountByAccountId(UUID accountId, UUID id) {
        var account = accountRepository.findAccountByIdAndUserId(accountId.toString(), id.toString()).orElseThrow(() -> NotFoundException.of("Account", accountId));
        return accountMapper.toView(account);
    }

    public List<AccountResponse> getAllAccountsByUserId(UUID userId) {
        var accounts = accountRepository.findAccountsByUserId(userId.toString());
        return accounts.stream().map(accountMapper::toView).collect(Collectors.toList());
    }

    @Transactional
    public void updateAccountStatus(UUID accountId, UpdateAccountStatusRequest request) {
        var account = accountRepository.findById(accountId).orElseThrow(() -> NotFoundException.of("Account", accountId));
        int rowsUpdated = accountRepository.updateAccountStatus(accountId.toString(), request.getStatus().name(), account.getVersion());
        if (rowsUpdated == 0) {
            throw new ConflictException("This account was updated by another request please retry.");
        }
        logger.info("account updated={}", account.getAccountNumber());
    }


}
