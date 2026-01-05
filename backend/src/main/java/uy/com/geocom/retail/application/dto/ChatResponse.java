package uy.com.geocom.retail.application.dto;

import java.time.Instant;

/**
 * DTO for chat responses to the frontend.
 */
public record ChatResponse(
    String content,
    Instant timestamp,
    String model
) {
    public ChatResponse(String content) {
        this(content, Instant.now(), null);
    }
}
