package com.expatica.todoservice.config;

import jakarta.validation.ClockProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class TestClockConfiguration {

    @Profile("test")
    @Bean
    public ClockProvider getClockProvider() {
        Instant fixed = Instant.parse("2012-12-21T08:00:00Z");
        return () -> Clock.fixed(fixed, ZoneId.of("UTC"));
    }

}
