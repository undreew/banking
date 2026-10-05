package org.example.banking.repositories;

import org.example.banking.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);


    @Modifying
    @Query(nativeQuery = true, value = "UPDATE transactions SET status = :to" + " WHERE id = :id AND status = :from")
    int transitionStatus(@Param("id") String id, @Param("from") String from, @Param("to") String to);
}
