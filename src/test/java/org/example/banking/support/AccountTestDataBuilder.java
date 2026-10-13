package org.example.banking.support;

import org.example.banking.constants.accounts.AccountStatus;
import org.example.banking.constants.accounts.AccountType;
import org.example.banking.entities.Account;
import org.example.banking.entities.User;
import org.example.banking.utils.AccountNumberGenerator;

import java.math.BigDecimal;
import java.util.UUID;

// id is null by default so Hibernate assigns it normally on persist — pre-setting a
// non-null id here makes Hibernate treat the entity as detached rather than new, and
// persist()/save() throws. Call withId() only for pure in-memory tests.
public final class AccountTestDataBuilder {

    private UUID id;
    private String accountNumber = AccountNumberGenerator.generateAccountNumber();
    private User owner = UserTestDataBuilder.aUser().build();
    private AccountType type = AccountType.CHECKING;
    private BigDecimal balance = BigDecimal.ZERO;
    private AccountStatus status = AccountStatus.ACTIVE;
    private Long version = 0L;

    private AccountTestDataBuilder() {
    }

    public static AccountTestDataBuilder anAccount() {
        return new AccountTestDataBuilder();
    }

    public AccountTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public AccountTestDataBuilder withOwner(User owner) {
        this.owner = owner;
        return this;
    }

    public AccountTestDataBuilder withBalance(String balance) {
        this.balance = new BigDecimal(balance);
        return this;
    }

    public AccountTestDataBuilder withType(AccountType type) {
        this.type = type;
        return this;
    }

    public AccountTestDataBuilder frozen() {
        this.status = AccountStatus.FROZEN;
        return this;
    }

    public AccountTestDataBuilder closed() {
        this.status = AccountStatus.CLOSED;
        return this;
    }

    public Account build() {
        Account account = new Account();
        if (id != null) {
            account.setId(id);
        }
        account.setAccountNumber(accountNumber);
        account.setOwner(owner);
        account.setType(type);
        account.setBalance(balance);
        account.setStatus(status);
        account.setVersion(version);
        return account;
    }
}
