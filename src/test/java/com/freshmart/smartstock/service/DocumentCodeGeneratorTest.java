package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.repository.DocumentCounterRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentCodeGeneratorTest {

    /** 2026-11-20 06:30 in Ho Chi Minh City is still 2026-11-19 in UTC: the business day must win. */
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-19T23:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final LocalDate businessDay = LocalDate.of(2026, 11, 20);

    @Mock
    private DocumentCounterRepository counterRepository;

    @Test
    void firstDocumentOfTheDayGetsSequence001() {
        when(counterRepository.findLastSeq("PN", businessDay)).thenReturn(1);

        String code = new DocumentCodeGenerator(counterRepository, clock).next("PN");

        assertEquals("PN-20261120-001", code);
    }

    @Test
    void sequenceIsReadAfterTheAtomicIncrement() {
        when(counterRepository.findLastSeq("PX", businessDay)).thenReturn(5);

        String code = new DocumentCodeGenerator(counterRepository, clock).next("PX");

        assertEquals("PX-20261120-005", code);
        InOrder order = inOrder(counterRepository);
        order.verify(counterRepository).incrementOrStart("PX", businessDay);
        order.verify(counterRepository).findLastSeq("PX", businessDay);
    }
}
