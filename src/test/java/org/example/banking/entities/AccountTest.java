package org.example.banking.entities;

import org.example.banking.exceptions.ConflictException;
import org.example.banking.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.example.banking.support.AccountTestDataBuilder.anAccount;
import static org.example.banking.support.UserTestDataBuilder.aUser;

class AccountTest {

    @Test
    void depositIncreasesBalance() {
        Account account = anAccount().withBalance("100.00").build();

        account.deposit(new BigDecimal("50.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    void withdrawDecreasesBalance() {
        Account account = anAccount().withBalance("100.00").build();

        account.withdraw(new BigDecimal("30.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void withdrawMoreThanBalanceThrowsConflict() {
        Account account = anAccount().withBalance("20.00").build();

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("20.01")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void depositOnFrozenAccountThrowsConflict() {
        Account account = anAccount().frozen().withBalance("100.00").build();

        assertThatThrownBy(() -> account.deposit(BigDecimal.TEN))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void withdrawOnClosedAccountThrowsConflict() {
        Account account = anAccount().closed().withBalance("100.00").build();

        assertThatThrownBy(() -> account.withdraw(BigDecimal.TEN))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void requireOwnerPassesForTheActualOwner() {
        var owner = aUser().withId(UUID.randomUUID()).build();
        Account account = anAccount().withOwner(owner).build();

        account.requireOwner(owner.getId());
    }

    @Test
    void requireOwnerThrowsForAnyoneElse() {
        var owner = aUser().withId(UUID.randomUUID()).build();
        Account account = anAccount().withOwner(owner).build();

        assertThatThrownBy(() -> account.requireOwner(UUID.randomUUID()))
                .isInstanceOf(UnauthorizedException.class);
    }
}
