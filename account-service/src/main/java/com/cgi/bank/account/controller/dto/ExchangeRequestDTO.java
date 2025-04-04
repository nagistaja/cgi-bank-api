package com.cgi.bank.account.controller.dto;

import java.math.BigDecimal;

import com.cgi.bank.account.domain.Currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for currency exchange request.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for converting money between currencies")
public class ExchangeRequestDTO {
    
    @NotNull(message = "From currency is required")
    @Schema(description = "Source currency code (EUR, USD, SEK, RUB)", example = "USD", required = true)
    private Currency fromCurrency;
    
    @NotNull(message = "To currency is required")
    @Schema(description = "Target currency code (EUR, USD, SEK, RUB)", example = "EUR", required = true)
    private Currency toCurrency;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Schema(description = "Amount to exchange in source currency", example = "100.00", required = true)
    private BigDecimal amount;
} 