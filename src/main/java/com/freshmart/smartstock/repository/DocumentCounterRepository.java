package com.freshmart.smartstock.repository;

import com.freshmart.smartstock.domain.entity.DocumentCounter;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DocumentCounterRepository extends JpaRepository<DocumentCounter, Long> {

    /** Locks the day's counter row so concurrent creators get distinct sequence numbers. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentCounter> findByPrefixAndCounterDate(String prefix, LocalDate counterDate);
}
