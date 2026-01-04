package com.geocom.retail.infrastructure.adapter.in.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Health check controller.
 * Provides endpoints for monitoring service health.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Basic health check.
     */
    @GetMapping
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString(),
            "service", "geo-retail-analytics"
        ));
    }

    /**
     * Detailed health check including database connectivity.
     */
    @GetMapping("/detailed")
    public Mono<Map<String, Object>> detailedHealth() {
        return Mono.fromCallable(() -> {
            boolean clickhouseOk = checkClickHouse();

            String status = clickhouseOk ? "UP" : "DEGRADED";

            return Map.of(
                "status", status,
                "timestamp", Instant.now().toString(),
                "service", "geo-retail-analytics",
                "components", Map.of(
                    "clickhouse", Map.of(
                        "status", clickhouseOk ? "UP" : "DOWN"
                    )
                )
            );
        });
    }

    private boolean checkClickHouse() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.warn("ClickHouse health check failed: {}", e.getMessage());
            return false;
        }
    }
}
