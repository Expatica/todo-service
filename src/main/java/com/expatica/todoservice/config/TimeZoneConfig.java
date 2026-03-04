package com.expatica.todoservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void init() {
        // Set JVM default timezone to UTC so JDBC drivers/Hibernate operate in UTC by default
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
