package com.cgi.bank.account.controller;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cgi.bank.account.controller.dto.AccountBalanceResponseDTO;
import com.cgi.bank.account.controller.dto.DepositRequestDTO;
import com.cgi.bank.account.controller.dto.WithdrawRequestDTO;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.integration.AbstractIntegrationTest;
import com.cgi.bank.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for AccountController.
 * Tests actual HTTP endpoints with a real database (via Testcontainers).
 */
@AutoConfigureMockMvc
@Tag("integration")
class AccountControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountService accountService;

    @Nested
    @DisplayName("Account balance retrieval")
    class GetAccountBalances {
        private String testAccountId;
        
        @BeforeEach
        void setUp() {
            AccountBalanceResponseDTO newAccount = accountService.createAccount();
            testAccountId = newAccount.getAccountId();
            
            accountService.deposit(testAccountId, new BigDecimal("100.00"), Currency.EUR);
        }
        
        @Test
        void returnsNotFoundWhenAccountDoesNotExist() throws Exception {
            mockMvc.perform(get("/api/v1/accounts/{accountId}/balances", "non-existent-id"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error", containsString("not found")));
        }

        @Test
        void returnsBalancesWhenAccountExists() throws Exception {
            mockMvc.perform(get("/api/v1/accounts/{accountId}/balances", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId", is(testAccountId)))
                    .andExpect(jsonPath("$.balances", hasSize(1)))
                    .andExpect(jsonPath("$.balances[0].currency", is("EUR")))
                    .andExpect(jsonPath("$.balances[0].amount", comparesEqualTo(100.0)));
        }
    }

    @Nested
    @DisplayName("Deposit operations")
    class DepositOperations {
        private String testDepositAccountId;
        
        @BeforeEach
        void setUp() {
            AccountBalanceResponseDTO newAccount = accountService.createAccount();
            testDepositAccountId = newAccount.getAccountId();
            
            accountService.deposit(testDepositAccountId, new BigDecimal("100.00"), Currency.EUR);
        }
        
        @Test
        void createsAccountAndBalanceOnSuccessfulDeposit() throws Exception {
            AccountBalanceResponseDTO newAccount = accountService.createAccount();
            String newAccountId = newAccount.getAccountId();
            
            DepositRequestDTO request = new DepositRequestDTO(new BigDecimal("150.00"), Currency.USD);
            
            mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", newAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId", is(newAccountId)))
                    .andExpect(jsonPath("$.balances", hasSize(1)))
                    .andExpect(jsonPath("$.balances[0].currency", is("USD")))
                    .andExpect(jsonPath("$.balances[0].amount", comparesEqualTo(150.0)));
        }
    
        @Test
        void addsToExistingBalanceOnSuccessfulDeposit() throws Exception {
            DepositRequestDTO request = new DepositRequestDTO(new BigDecimal("50.00"), Currency.EUR);
    
            mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", testDepositAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId", is(testDepositAccountId)))
                    .andExpect(jsonPath("$.balances", hasSize(1)))
                    .andExpect(jsonPath("$.balances[0].currency", is("EUR")))
                    .andExpect(jsonPath("$.balances[0].amount", comparesEqualTo(150.0)));
        }
    }

    @Nested
    @DisplayName("Withdrawal operations")
    class WithdrawalOperations {
        private String testWithdrawAccountId;
        
        @BeforeEach
        void setUp() {
            AccountBalanceResponseDTO newAccount = accountService.createAccount();
            testWithdrawAccountId = newAccount.getAccountId();
            
            accountService.deposit(testWithdrawAccountId, new BigDecimal("100.00"), Currency.EUR);
        }
        
        @Test
        void subtractsBalanceOnSuccessfulWithdrawal() throws Exception {
            WithdrawRequestDTO request = new WithdrawRequestDTO(new BigDecimal("50.00"), Currency.EUR);
    
            mockMvc.perform(post("/api/v1/accounts/{accountId}/withdrawals", testWithdrawAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId", is(testWithdrawAccountId)))
                    .andExpect(jsonPath("$.balances", hasSize(1)))
                    .andExpect(jsonPath("$.balances[0].currency", is("EUR")))
                    .andExpect(jsonPath("$.balances[0].amount", comparesEqualTo(50.0)));
        }
    
        @Test
        void returnsUnprocessableEntityOnInsufficientFunds() throws Exception {
            WithdrawRequestDTO request = new WithdrawRequestDTO(new BigDecimal("150.00"), Currency.EUR);
    
            mockMvc.perform(post("/api/v1/accounts/{accountId}/withdrawals", testWithdrawAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error", is("Insufficient funds")));
        }
    }
} 