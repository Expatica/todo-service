package com.expatica.todoservice.config;

import com.expatica.todoservice.util.TimeProvider;
import jakarta.validation.ClockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
public class ClockConfiguration {

    @Bean
    @Profile("!test")
    public ClockProvider clockProvider() {
        return Clock::systemUTC;
    }

    @Bean
    public TimeProvider timeProvider(ClockProvider clockProvider) {
        return () -> clockProvider.getClock().instant();
    }

}
