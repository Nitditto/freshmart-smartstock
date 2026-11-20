package com.freshmart.smartstock.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A single injectable Clock keeps "what day is it" deterministic in tests
 * (document codes PN-yyyyMMdd-xxx, report date ranges).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(AppProperties props) {
        return Clock.system(ZoneId.of(props.timezone()));
    }
}
