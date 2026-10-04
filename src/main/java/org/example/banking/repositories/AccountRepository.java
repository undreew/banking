package org.example.banking.repositories;

import org.example.banking.entities.Account;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    
    // find all account from the user id for faster
    @Query(nativeQuery = true, value = "SELECT * FROM accounts WHERE user_id = :userId")
    List<Account> findAccountsByUserId(@Param("userId") String userId);

    // update account status
    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = "UPDATE accounts SET status = :status " + ", version = version + 1 " + " WHERE id = :id AND version = :version")
    int updateAccountStatus(@Param("id") String id, @Param("status") String status, @Param("version") Long version);
}
