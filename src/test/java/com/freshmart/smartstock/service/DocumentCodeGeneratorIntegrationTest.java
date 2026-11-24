package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.freshmart.smartstock.support.AbstractIntegrationTest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** The race the old find-then-insert code lost: many first documents of the same day, created at once. */
class DocumentCodeGeneratorIntegrationTest extends AbstractIntegrationTest {

    private static final int THREADS = 20;
    private static final int TIMEOUT_SECONDS = 60;

    @Autowired
    private DocumentCodeGenerator generator;
    @Autowired
    private Clock clock;

    @Test
    void concurrentCallersGetDistinctConsecutiveCodes() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    return generator.next(DocumentCodeGenerator.ISSUE_PREFIX);
                }));
            }
            go.countDown();
            List<String> codes = new ArrayList<>();
            for (Future<String> future : futures) {
                codes.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }

            String day = LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE);
            List<String> expected = IntStream.rangeClosed(1, THREADS)
                    .mapToObj(n -> "PX-%s-%03d".formatted(day, n)).toList();
            assertEquals(expected, codes.stream().sorted().toList());
        } finally {
            pool.shutdownNow();
        }
    }
}
