package org.example.banking.concurrency;

import org.example.banking.constants.transactions.TransactionStatus;
import org.example.banking.constants.transactions.TransactionType;
import org.example.banking.dtos.transaction.WithdrawRequest;
import org.example.banking.entities.Account;
import org.example.banking.entities.User;
import org.example.banking.repositories.AccountRepository;
import org.example.banking.repositories.TransactionRepository;
import org.example.banking.repositories.UserRepository;
import org.example.banking.services.TokenService;
import org.example.banking.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.banking.support.AccountTestDataBuilder.anAccount;
import static org.example.banking.support.UserTestDataBuilder.aUser;

// The exact scenario Section 8 of the spec promises a guard for: two requests racing
// to update the same Account row must never both win. Everything here is real — a
// real embedded server, a real HTTP call per attempt, a real MySQL container.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ConcurrencyIT extends AbstractIntegrationTest {

    // Capped at withdraw's real @WithRateLimit default (5/min, keyed by IP+URI) so
    // this test measures the @Version race, not the rate limiter — every one of these
    // requests shares the same client IP and the same account URI.
    private static final int CONCURRENT_REQUESTS = 5;
    private static final BigDecimal WITHDRAW_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal STARTING_BALANCE = new BigDecimal("10000.00");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    TokenService tokenService;

    @Test
    void parallelWithdrawalsOnOneAccountKeepTheMoneyCorrect() throws InterruptedException {
        User owner = userRepository.save(aUser().build());
        Account account = accountRepository.save(anAccount()
                .withOwner(owner)
                .withBalance(STARTING_BALANCE.toPlainString())
                .build());
        String accessToken = tokenService.issueAccessToken(owner, Instant.now());

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        try {
            List<Callable<Integer>> attempts = IntStream.range(0, CONCURRENT_REQUESTS)
                    .<Callable<Integer>>mapToObj(i -> () -> {
                        startGate.await();
                        return attemptWithdraw(account.getId(), accessToken);
                    })
                    .toList();

            List<Future<Integer>> futures = attempts.stream().map(pool::submit).toList();
            startGate.countDown(); // release every thread at once to maximize overlap

            long successes = 0;
            long conflicts = 0;
            for (Future<Integer> future : futures) {
                int status = awaitStatus(future);
                if (status == 200) {
                    successes++;
                } else if (status == 409) {
                    conflicts++;
                } else {
                    throw new AssertionError("Unexpected status code: " + status);
                }
            }

            assertThat(successes + conflicts).isEqualTo(CONCURRENT_REQUESTS);

            Account reloaded = accountRepository.findById(account.getId()).orElseThrow();
            BigDecimal expectedBalance = STARTING_BALANCE.subtract(
                    WITHDRAW_AMOUNT.multiply(BigDecimal.valueOf(successes)));
            assertThat(reloaded.getBalance()).isEqualByComparingTo(expectedBalance);

            long completedWithdrawals = transactionRepository.findAll().stream()
                    .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                    .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                    .filter(t -> t.getSourceAccount() != null && t.getSourceAccount().getId().equals(account.getId()))
                    .count();
            assertThat(completedWithdrawals).isEqualTo(successes);
        } finally {
            pool.shutdownNow();
        }
    }

    private int attemptWithdraw(UUID accountId, String accessToken) {
        var request = new WithdrawRequest();
        request.setAmount(WITHDRAW_AMOUNT);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/accounts/" + accountId + "/withdraw",
                HttpMethod.POST,
                new HttpEntity<>(jsonMapper.writeValueAsString(request), headers),
                String.class);

        return response.getStatusCode().value();
    }

    private int awaitStatus(Future<Integer> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new AssertionError("Withdraw attempt failed to complete", e);
        }
    }
}
