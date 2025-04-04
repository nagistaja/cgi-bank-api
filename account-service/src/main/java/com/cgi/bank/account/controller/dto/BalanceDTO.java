package com.cgi.bank.account.controller.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single currency balance.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Currency balance information")
public class BalanceDTO {
    @Schema(description = "Currency code", example = "EUR")
    private String currency;
    
    @Schema(description = "Current balance amount", example = "150.50")
    private BigDecimal amount;
} 