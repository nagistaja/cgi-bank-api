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
 * DTO for withdrawal request.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for withdrawing money from an account")
public class WithdrawRequestDTO {
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Schema(description = "Amount to withdraw", example = "50.00", required = true)
    private BigDecimal amount;
    
    @NotNull(message = "Currency is required")
    @Schema(description = "Currency code (EUR, USD, SEK, RUB)", example = "EUR", required = true)
    private Currency currency;
} 