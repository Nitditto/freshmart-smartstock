package com.freshmart.smartstock.service;

import com.freshmart.smartstock.repository.DocumentCounterRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates PN-yyyyMMdd-xxx (receipts) and PX-yyyyMMdd-xxx (issues). Counter resets each day because the
 * counter row is keyed by (prefix, date). Call it inside the same transaction that saves the document so a
 * failed save does not burn a number.
 */
@Component
@RequiredArgsConstructor
public class DocumentCodeGenerator {

    public static final String RECEIPT_PREFIX = "PN";
    public static final String ISSUE_PREFIX = "PX";

    private final DocumentCounterRepository counterRepository;
    private final Clock clock;

    @Transactional
    public String next(String prefix) {
        LocalDate today = LocalDate.now(clock);
        // An upsert instead of "find, else insert": catching the duplicate-key error inside the caller's
        // transaction cannot work (Hibernate marks it rollback-only), and two empty FOR UPDATE reads deadlock.
        counterRepository.incrementOrStart(prefix, today);
        int sequence = counterRepository.findLastSeq(prefix, today);
        return "%s-%s-%03d".formatted(prefix, today.format(DateTimeFormatter.BASIC_ISO_DATE), sequence);
    }
}
