package org.example.banking.repositories;

import org.example.banking.constants.accounts.AccountStatus;
import org.example.banking.entities.Account;
import org.example.banking.entities.User;
import org.example.banking.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.banking.support.AccountTestDataBuilder.anAccount;
import static org.example.banking.support.UserTestDataBuilder.aUser;

// replace = NONE: @DataJpaTest defaults to an embedded database, which would silently
// swap out the real MySQL container these queries are native SQL written against.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    AccountRepository accountRepository;

    // Arrange-phase fixtures go through TestEntityManager.persist(), not
    // *Repository.save(): the builders give entities a pre-set id, and Spring Data's
    // save() treats a non-null id as "already exists" and issues an UPDATE instead of
    // an INSERT — which affects 0 rows and throws, since nothing exists yet.
    private User persistedUser() {
        return entityManager.persist(aUser().build());
    }

    private Account persistedAccount(User owner) {
        return entityManager.persistFlushFind(anAccount().withOwner(owner).build());
    }

    @Test
    void findAccountsByUserIdReturnsOnlyThatUsersAccounts() {
        User owner = persistedUser();
        User someoneElse = persistedUser();

        Account ownersAccount = persistedAccount(owner);
        persistedAccount(someoneElse);

        var result = accountRepository.findAccountsByUserId(owner.getId().toString());

        assertThat(result).extracting(Account::getId).containsExactly(ownersAccount.getId());
    }

    @Test
    void updateAccountStatusWithCorrectVersionSucceeds() {
        Account account = persistedAccount(persistedUser());

        int rows = accountRepository.updateAccountStatus(
                account.getId().toString(), AccountStatus.FROZEN.name(), account.getVersion());

        assertThat(rows).isEqualTo(1);
        assertThat(accountRepository.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.FROZEN);
    }

    @Test
    void updateAccountStatusWithStaleVersionUpdatesNothing() {
        Account account = persistedAccount(persistedUser());
        long callerVersion = account.getVersion(); // what a concurrent caller still believes

        // a first writer updates the row, bumping its real version ahead of the caller
        accountRepository.updateAccountStatus(account.getId().toString(), AccountStatus.FROZEN.name(), callerVersion);

        // the stale caller retries with the version it still has
        int rows = accountRepository.updateAccountStatus(
                account.getId().toString(), AccountStatus.CLOSED.name(), callerVersion);

        assertThat(rows).isZero();
        assertThat(accountRepository.findById(account.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.FROZEN);
    }
}
