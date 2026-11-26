package com.freshmart.smartstock.repository;

import com.freshmart.smartstock.domain.entity.DocumentCounter;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentCounterRepository extends JpaRepository<DocumentCounter, Long> {

    /**
     * Creates the day's counter at 1 or increments it, in ONE statement. MySQL locks the (prefix, counter_date)
     * key until the caller's transaction ends, so concurrent creators are serialized instead of racing on the
     * "first document of the day" insert.
     */
    @Modifying
    @Query(value = """
            insert into document_counters (prefix, counter_date, last_seq) values (:prefix, :day, 1)
            on duplicate key update last_seq = last_seq + 1
            """, nativeQuery = true)
    void incrementOrStart(@Param("prefix") String prefix, @Param("day") LocalDate day);

    /** Reads the value this transaction just wrote (a transaction always sees its own writes). */
    @Query(value = "select last_seq from document_counters where prefix = :prefix and counter_date = :day",
            nativeQuery = true)
    int findLastSeq(@Param("prefix") String prefix, @Param("day") LocalDate day);
}
