package com.cgi.bank.account.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.cgi.bank.account.domain.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning transaction information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction details")
public class TransactionResponseDTO {
    
    @Schema(description = "Unique transaction ID", example = "12345")
    private Long id;
    
    @Schema(description = "Account ID associated with the transaction", example = "acc-123456")
    private String accountId;
    
    @Schema(description = "Type of transaction: DEPOSIT, WITHDRAWAL, EXCHANGE_FROM, EXCHANGE_TO", example = "DEPOSIT")
    private TransactionType type;
    
    @Schema(description = "Amount involved in the transaction", example = "100.50")
    private BigDecimal amount;
    
    @Schema(description = "Currency code of the transaction", example = "USD")
    private String currency;
    
    @Schema(description = "Related currency for exchange transactions", example = "EUR")
    private String relatedCurrency;
    
    @Schema(description = "Date and time when the transaction occurred")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    private Instant timestamp;
    
    @Schema(description = "Description or reference for the transaction", example = "Cash deposit")
    private String description;
} 