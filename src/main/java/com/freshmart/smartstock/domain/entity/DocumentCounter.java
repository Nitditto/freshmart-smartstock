package com.freshmart.smartstock.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per (prefix, day). Because the date is part of the key, the sequence restarts at 1 every day
 * without any scheduled job.
 */
@Entity
@Table(name = "document_counters")
@Getter
@Setter
@NoArgsConstructor
public class DocumentCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5)
    private String prefix;

    @Column(name = "counter_date", nullable = false)
    private LocalDate counterDate;

    @Column(name = "last_seq", nullable = false)
    private int lastSeq;

    public DocumentCounter(String prefix, LocalDate counterDate) {
        this.prefix = prefix;
        this.counterDate = counterDate;
        this.lastSeq = 0;
    }
}
