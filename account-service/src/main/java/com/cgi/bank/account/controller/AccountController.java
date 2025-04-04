package com.cgi.bank.account.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.cgi.bank.account.controller.dto.AccountBalanceResponseDTO;
import com.cgi.bank.account.controller.dto.DepositRequestDTO;
import com.cgi.bank.account.controller.dto.ErrorResponseDTO;
import com.cgi.bank.account.controller.dto.ExchangeRequestDTO;
import com.cgi.bank.account.controller.dto.TransactionResponseDTO;
import com.cgi.bank.account.controller.dto.WithdrawRequestDTO;
import com.cgi.bank.account.service.AccountService;
import com.cgi.bank.account.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for account-related operations.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Management", description = "APIs for managing bank accounts and balances")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    /**
     * Creates a new empty account.
     *
     * @return ResponseEntity containing the account and its balances
     */
    @Operation(summary = "Create new account", description = "Creates a new empty bank account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = AccountBalanceResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<AccountBalanceResponseDTO> createAccount() {
        log.info("REST request to create a new account");
        AccountBalanceResponseDTO response = accountService.createAccount();
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getAccountId())
                .toUri();
        
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Gets all balances for a specific account.
     *
     * @param accountId the ID of the account
     * @return ResponseEntity containing the account and its balances
     */
    @Operation(summary = "Get account balances", description = "Retrieves all currency balances for a specific account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balances retrieved successfully",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = AccountBalanceResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{accountId}/balances")
    public ResponseEntity<AccountBalanceResponseDTO> getAccountBalances(@PathVariable String accountId) {
        log.info("REST request to get balances for account: {}", accountId);
        AccountBalanceResponseDTO response = accountService.getAccountBalances(accountId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Deposits money into an account in the specified currency.
     *
     * @param accountId the ID of the account
     * @param requestDTO the deposit request containing amount and currency
     * @return ResponseEntity containing the account and its updated balances
     */
    @Operation(summary = "Deposit money", description = "Adds money to an account in the specified currency")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deposit successful",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = AccountBalanceResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request (negative amount, invalid currency)",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{accountId}/deposits")
    public ResponseEntity<AccountBalanceResponseDTO> deposit(
            @PathVariable String accountId,
            @Valid @RequestBody DepositRequestDTO requestDTO) {
        
        log.info("REST request to deposit {} {} into account: {}", 
                requestDTO.getAmount(), requestDTO.getCurrency(), accountId);
        
        AccountBalanceResponseDTO response = accountService.deposit(
                accountId, requestDTO.getAmount(), requestDTO.getCurrency());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Withdraws money from an account in the specified currency.
     *
     * @param accountId the ID of the account
     * @param requestDTO the withdrawal request containing amount and currency
     * @return ResponseEntity containing the account and its updated balances
     */
    @Operation(summary = "Withdraw money", description = "Withdraws money from an account in the specified currency")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Withdrawal successful",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = AccountBalanceResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient funds",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Concurrent modification conflict",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{accountId}/withdrawals")
    public ResponseEntity<AccountBalanceResponseDTO> withdraw(
            @PathVariable String accountId,
            @Valid @RequestBody WithdrawRequestDTO requestDTO) {
        
        log.info("REST request to withdraw {} {} from account: {}", 
                requestDTO.getAmount(), requestDTO.getCurrency(), accountId);
        
        AccountBalanceResponseDTO response = accountService.withdraw(
                accountId, requestDTO.getAmount(), requestDTO.getCurrency());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Exchanges money from one currency to another within an account.
     *
     * @param accountId the ID of the account
     * @param requestDTO the exchange request containing from/to currencies and amount
     * @return ResponseEntity containing the account and its updated balances
     */
    @Operation(summary = "Exchange currency", 
                description = "Converts money from one currency to another within an account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange successful",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = AccountBalanceResponseDTO.class))),
            @ApiResponse(responseCode = "400", 
                    description = "Invalid request, insufficient funds, or unsupported currency pair",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Concurrent modification conflict",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{accountId}/exchanges")
    public ResponseEntity<AccountBalanceResponseDTO> exchange(
            @PathVariable String accountId,
            @Valid @RequestBody ExchangeRequestDTO requestDTO) {
        
        log.info("REST request to exchange {} {} to {} for account: {}", 
                requestDTO.getAmount(), requestDTO.getFromCurrency(), requestDTO.getToCurrency(), accountId);
        
        AccountBalanceResponseDTO response = accountService.exchange(
                accountId, 
                requestDTO.getFromCurrency(), 
                requestDTO.getToCurrency(), 
                requestDTO.getAmount());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets transaction history for a specific account.
     *
     * @param accountId the ID of the account
     * @param page the page number (0-based)
     * @param size the page size
     * @return ResponseEntity containing the list of transactions
     */
    @Operation(summary = "Get transaction history", 
            description = "Retrieves transaction history for a specific account with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = TransactionResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<Page<TransactionResponseDTO>> getTransactionHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("REST request to get transaction history for account: {}, page: {}, size: {}", 
                accountId, page, size);
        
        Page<TransactionResponseDTO> transactions = transactionService.getTransactionHistory(
                accountId, page, size);
        
        return ResponseEntity.ok(transactions);
    }
} 