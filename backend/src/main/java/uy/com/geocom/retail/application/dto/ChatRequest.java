package uy.com.geocom.retail.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for chat requests from the frontend.
 */
public record ChatRequest(
    @NotBlank(message = "Message cannot be blank")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    String message,

    Long companyId
) {
    public ChatRequest {
        if (companyId == null) {
            companyId = 1L;  // Default company
        }
    }
}
