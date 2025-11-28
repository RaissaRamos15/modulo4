package com.rairai.consumer_kafka_2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to register Jackson modules and customize ObjectMapper behavior.
 * Registers {@link JavaTimeModule} so java.time types (e.g. Instant) are serialized/deserialized
 * as ISO-8601 strings instead of timestamps.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Support java.time.* (Instant, LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        // Prefer ISO-8601 string representation for dates instead of numeric timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}