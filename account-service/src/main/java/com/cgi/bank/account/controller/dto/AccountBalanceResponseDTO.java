package com.cgi.bank.account.controller.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an account with its currency balances.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response containing account information and balances")
public class AccountBalanceResponseDTO {
    @Schema(description = "Unique identifier of the account", example = "f7e9a1b2-c3d4-5e6f-7a8b-9c0d1e2f3a4b")
    private String accountId;
    
    @Schema(description = "List of currency balances in the account")
    private List<BalanceDTO> balances;
} 