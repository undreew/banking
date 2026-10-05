package org.example.banking.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.banking.constants.transactions.TransactionStatus;
import org.example.banking.constants.transactions.TransactionType;
import org.example.banking.dtos.auth.AuthenticatedActor;
import org.example.banking.dtos.transaction.*;
import org.example.banking.entities.Account;
import org.example.banking.entities.Transaction;
import org.example.banking.exceptions.ConflictException;
import org.example.banking.exceptions.NotFoundException;
import org.example.banking.mappers.TransactionMapper;
import org.example.banking.repositories.AccountRepository;
import org.example.banking.repositories.TransactionRepository;
import org.example.banking.repositories.TransactionSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountRepository accountRepository;

    public Page<TransactionResponse> getAllTransactions(TransactionFilter filter, AuthenticatedActor actor, Pageable pageable) {
        UUID scopedAccountId = actor.role().equals("ADMIN") ? filter.id() : requireAccountIdFor(actor, filter);
        Specification<Transaction> spec = TransactionSpecifications.withFilter(filter, scopedAccountId);
        Page<Transaction> transactions = transactionRepository.findAll(spec, pageable);

        return transactions.map(transactionMapper::toView);
    }

    @Transactional
    public TransactionResponse reverseTransaction(UUID id, String idempotencyKey, ReverseTransactionRequest request) {
        var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            throw new ConflictException("Existing reverse transaction");
        }

        var originalTransaction = transactionRepository.findById(id).orElseThrow(() -> NotFoundException.of("Account", id));
        if (originalTransaction.getType() != TransactionType.TRANSFER) {
            throw new ConflictException("Only transfers can be reversed");
        }
        
        int rows = transactionRepository.transitionStatus(id.toString(), TransactionStatus.COMPLETED.name(), TransactionStatus.REVERSED.name());
        if (rows == 0) {
            throw new ConflictException("Transaction cannot be reversed (already reversed, or not completed)");
        }

        moveFundsBack(originalTransaction);

        Transaction transaction = Transaction.create(originalTransaction.getAmount(), TransactionType.TRANSFER, TransactionStatus.COMPLETED, request.getReason(), originalTransaction.getDestinationAccount(), originalTransaction.getSourceAccount());
        transaction.addReversalOfTransaction(originalTransaction);
        transaction.addIdempotencyKey(idempotencyKey);

        transactionRepository.saveAndFlush(transaction);

        return transactionMapper.toView(originalTransaction);
    }

    @Transactional
    public TransactionResponse transfer(UUID id, String idempotencyKey, TransferRequest request) {
        UUID requestedSourceId = request.getSourceAccountId();
        UUID requestedDestinationId = request.getDestinationAccountId();

        UUID firstToLoad = requestedSourceId.compareTo(requestedDestinationId) < 0 ? requestedSourceId : requestedDestinationId;
        UUID secondToLoad = firstToLoad.equals(requestedSourceId) ? requestedDestinationId : requestedSourceId;

        var firstAccount = accountRepository.findById(firstToLoad).orElseThrow(() -> NotFoundException.of("Account", firstToLoad));
        var secondAccount = accountRepository.findById(secondToLoad).orElseThrow(() -> NotFoundException.of("Account", secondToLoad));

        var sourceAccount = firstAccount.getId().equals(requestedSourceId) ? firstAccount : secondAccount;
        var destinationAccount = firstAccount.getId().equals(requestedSourceId) ? secondAccount : firstAccount;

        sourceAccount.requireOwner(id);

        sourceAccount.withdraw(request.getAmount());
        destinationAccount.deposit(request.getAmount());

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        Transaction transaction = Transaction.create(request.getAmount(), TransactionType.TRANSFER, TransactionStatus.COMPLETED, request.getDescription(), sourceAccount, destinationAccount);
        transaction.addIdempotencyKey(idempotencyKey);
        transactionRepository.saveAndFlush(transaction);

        logger.info("Transfer from {} to {}", sourceAccount, destinationAccount);
        return transactionMapper.toView(transaction);
    }

    @Transactional
    public TransactionResponse depositTransaction(UUID id, UUID actorId, String idempotencyKey, DepositRequest request) {
        var account = accountRepository.findById(id).orElseThrow(() -> NotFoundException.of("Account", id));

        account.requireOwner(actorId);
        account.deposit(request.getAmount());

        accountRepository.save(account);
        logger.info("deposited amount={}", request.getAmount());

        Transaction transaction = Transaction.create(request.getAmount(), TransactionType.DEPOSIT, TransactionStatus.COMPLETED, request.getDescription(), null, account

        );
        transaction.addIdempotencyKey(idempotencyKey);
        transactionRepository.saveAndFlush(transaction);
        logger.info("transaction for deposit created={}", transaction.getDestinationAccount().getId());

        return transactionMapper.toView(transaction);
    }

    @Transactional
    public TransactionResponse withdrawTransaction(UUID id, UUID actorId, String idempotencyKey, WithdrawRequest request) {
        var account = accountRepository.findById(id).orElseThrow(() -> NotFoundException.of("Account", id));

        account.requireOwner(actorId);
        account.withdraw(request.getAmount());

        accountRepository.save(account);
        logger.info("withdrawn amount={}", request.getAmount());

        Transaction transaction = Transaction.create(request.getAmount(), TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED, request.getDescription(), account, null);
        transaction.addIdempotencyKey(idempotencyKey);
        transactionRepository.saveAndFlush(transaction);
        logger.info("transaction for withdraw created={}", transaction.getSourceAccount().getId());

        return transactionMapper.toView(transaction);
    }

    private UUID requireAccountIdFor(AuthenticatedActor actor, TransactionFilter filter) {
        if (filter.id() == null) throw NotFoundException.of("Account", filter.id());

        Account account = accountRepository.findById(filter.id()).orElseThrow(() -> NotFoundException.of("Account", filter.id()));
        account.requireOwner(actor.id());

        return filter.id();
    }

    private void moveFundsBack(Transaction original) {
        Account from = original.getDestinationAccount();
        Account to = original.getSourceAccount();

        from.withdraw(original.getAmount());
        to.deposit(original.getAmount());
        accountRepository.save(from);
        accountRepository.save(to);
    }
}
