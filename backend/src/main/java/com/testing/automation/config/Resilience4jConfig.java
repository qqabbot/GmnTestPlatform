package com.testing.automation.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Configuration
 * Circuit breaker and retry are configured programmatically in services
 */
@Configuration
public class Resilience4jConfig {
    // Configuration is done programmatically in TestCaseService
    // This class exists for future centralized configuration
}
