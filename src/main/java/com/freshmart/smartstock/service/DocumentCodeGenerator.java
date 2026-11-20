package com.freshmart.smartstock.service;

import com.freshmart.smartstock.domain.entity.DocumentCounter;
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
 *
 * TODO[Member C]: two requests creating the FIRST document of a day can both miss the row and insert it;
 * the unique key (prefix, counter_date) makes one fail with 409 - add a single retry or use
 * INSERT ... ON DUPLICATE KEY UPDATE.
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
        DocumentCounter counter = counterRepository.findByPrefixAndCounterDate(prefix, today)
                .orElseGet(() -> counterRepository.saveAndFlush(new DocumentCounter(prefix, today)));
        counter.setLastSeq(counter.getLastSeq() + 1);
        return "%s-%s-%03d".formatted(prefix, today.format(DateTimeFormatter.BASIC_ISO_DATE), counter.getLastSeq());
    }
}
