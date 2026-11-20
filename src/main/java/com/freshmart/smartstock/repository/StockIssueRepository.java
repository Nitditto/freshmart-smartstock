package com.freshmart.smartstock.repository;

import com.freshmart.smartstock.domain.entity.StockIssue;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockIssueRepository extends JpaRepository<StockIssue, Long> {

    @Override
    @EntityGraph(attributePaths = {"details", "details.product", "createdBy"})
    Optional<StockIssue> findById(Long id);

    /** from inclusive, to exclusive (pass start of next day). Any filter may be null. */
    @Query("""
            select i from StockIssue i
            where (:status is null or i.status = :status)
              and (:from is null or i.createdAt >= :from)
              and (:to is null or i.createdAt < :to)
            """)
    Page<StockIssue> search(@Param("status") DocumentStatus status,
                            @Param("from") Instant from,
                            @Param("to") Instant to,
                            Pageable pageable);
}
