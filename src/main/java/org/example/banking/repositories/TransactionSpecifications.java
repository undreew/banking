package org.example.banking.repositories;

import jakarta.persistence.criteria.Predicate;
import org.example.banking.dtos.transaction.TransactionFilter;
import org.example.banking.entities.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionSpecifications {

    public static Specification<Transaction> withFilter(TransactionFilter filter, UUID requiredAccountId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // scope to a specific account on EITHER side of the transaction
            if (requiredAccountId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("sourceAccount").get("id"), requiredAccountId),
                        cb.equal(root.get("destinationAccount").get("id"), requiredAccountId)
                ));
            }

            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }

            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }

            if (filter.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.dateFrom()));
            }

            if (filter.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.dateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
