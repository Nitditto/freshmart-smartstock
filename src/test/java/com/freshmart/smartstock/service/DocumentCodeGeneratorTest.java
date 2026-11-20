package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.domain.entity.DocumentCounter;
import com.freshmart.smartstock.repository.DocumentCounterRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentCodeGeneratorTest {

    // 2026-11-20 10:00 in Ho Chi Minh City
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-20T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private DocumentCounterRepository counterRepository;

    @Test
    void firstDocumentOfTheDayGetsSequence001() {
        LocalDate today = LocalDate.of(2026, 11, 20);
        when(counterRepository.findByPrefixAndCounterDate("PN", today)).thenReturn(Optional.empty());
        when(counterRepository.saveAndFlush(any(DocumentCounter.class))).thenAnswer(inv -> inv.getArgument(0));

        String code = new DocumentCodeGenerator(counterRepository, clock).next("PN");

        assertEquals("PN-20261120-001", code);
    }

    @Test
    void nextDocumentIncrementsTheDailyCounter() {
        LocalDate today = LocalDate.of(2026, 11, 20);
        DocumentCounter existing = new DocumentCounter("PX", today);
        existing.setLastSeq(4);
        when(counterRepository.findByPrefixAndCounterDate("PX", today)).thenReturn(Optional.of(existing));

        String code = new DocumentCodeGenerator(counterRepository, clock).next("PX");

        assertEquals("PX-20261120-005", code);
    }
}
