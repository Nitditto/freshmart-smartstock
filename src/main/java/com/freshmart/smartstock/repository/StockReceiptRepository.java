package com.freshmart.smartstock.repository;

import com.freshmart.smartstock.domain.entity.StockReceipt;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {

    /** Loads header + lines + product + supplier + creator in one query (avoids N+1 / lazy errors). */
    @Override
    @EntityGraph(attributePaths = {"details", "details.product", "supplier", "createdBy"})
    Optional<StockReceipt> findById(Long id);

    /**
     * Locks the receipt row and reads it fresh (no EntityGraph: lines load lazily AFTER the lock, so we never
     * see a stale snapshot under MySQL REPEATABLE READ).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StockReceipt r where r.id = :id")
    Optional<StockReceipt> findByIdForUpdate(@Param("id") Long id);

    /** from inclusive, to exclusive (pass start of next day). Any filter may be null. */
    @Query("""
            select r from StockReceipt r
            where (:status is null or r.status = :status)
              and (:from is null or r.createdAt >= :from)
              and (:to is null or r.createdAt < :to)
            """)
    Page<StockReceipt> search(@Param("status") DocumentStatus status,
                              @Param("from") Instant from,
                              @Param("to") Instant to,
                              Pageable pageable);
}
