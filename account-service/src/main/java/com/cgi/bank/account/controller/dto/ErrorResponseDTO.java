package com.cgi.bank.account.controller.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for standardized error responses.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Standard error response format")
public class ErrorResponseDTO {
    
    @Schema(description = "Timestamp when the error occurred", example = "2023-09-15T14:30:15.123")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private int status;
    
    @Schema(description = "Error type", example = "Not Found")
    private String error;
    
    @Schema(description = "Detailed error message", example = "Account with ID '12345' not found")
    private String message;
    
    @Schema(description = "Request path that triggered the error", example = "/api/v1/accounts/12345/balances")
    private String path;
} 