package org.example.banking.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.example.banking.repositories.RefreshTokenRepository;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanup {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(cron = "0 0 6 * * *")
    public void runDailyJob() {
        logger.info("Running refresh token cleanup");
        Instant cutoff = Instant.now().minus(Duration.ofDays(1));
        int deleted;
        do {
            deleted = transactionTemplate.execute(s -> refreshTokenRepository.deleteExpiredBefore(cutoff, 100));
        } while (deleted == 100);
    }
}
