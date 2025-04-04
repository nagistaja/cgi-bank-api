package com.cgi.bank.account.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.cgi.bank.account.controller.dto.AccountBalanceResponseDTO;
import com.cgi.bank.account.controller.dto.BalanceDTO;
import com.cgi.bank.account.controller.dto.DepositRequestDTO;
import com.cgi.bank.account.controller.dto.ExchangeRequestDTO;
import com.cgi.bank.account.controller.dto.TransactionResponseDTO;
import com.cgi.bank.account.controller.dto.WithdrawRequestDTO;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.domain.TransactionType;
import com.cgi.bank.account.exception.AccountNotFoundException;
import com.cgi.bank.account.exception.BalanceNotFoundException;
import com.cgi.bank.account.exception.InsufficientFundsException;
import com.cgi.bank.account.exception.InvalidCurrencyException;
import com.cgi.bank.account.exception.OptimisticLockingConflictException;
import com.cgi.bank.account.service.AccountService;
import com.cgi.bank.account.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for the AccountController.
 * Uses MockMvc to simulate HTTP requests and validate responses.
 */
@WebMvcTest(AccountController.class)
@Import({GlobalExceptionHandler.class})
@WithMockUser
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        public AccountService accountService() {
            return mock(AccountService.class);
        }
        
        @Bean
        public TransactionService transactionService() {
            return mock(TransactionService.class);
        }
    }
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private TransactionService transactionService;

    private final String testAccountId = "account123";
    
    @BeforeEach
    void setUp() {
        reset(accountService);
        reset(transactionService);
    }

    @Test
    void getAccountBalances_success_returnsBalances() throws Exception {
        BalanceDTO eurBalance = new BalanceDTO("EUR", new BigDecimal("100.00"));
        BalanceDTO usdBalance = new BalanceDTO("USD", new BigDecimal("150.00"));
        AccountBalanceResponseDTO response = 
                new AccountBalanceResponseDTO(testAccountId, List.of(eurBalance, usdBalance));
        
        when(accountService.getAccountBalances(testAccountId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/balances", testAccountId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(testAccountId)))
                .andExpect(jsonPath("$.balances", hasSize(2)))
                .andExpect(jsonPath("$.balances[0].currency", is("EUR")))
                .andExpect(jsonPath("$.balances[0].amount", is(100.00)))
                .andExpect(jsonPath("$.balances[1].currency", is("USD")))
                .andExpect(jsonPath("$.balances[1].amount", is(150.00)));
    }

    @Test
    void getAccountBalances_accountNotFound_returns404() throws Exception {
        doThrow(new AccountNotFoundException(testAccountId))
            .when(accountService).getAccountBalances(testAccountId);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/balances", testAccountId)
                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Account not found")))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deposit_success_returnsUpdatedBalances() throws Exception {
        DepositRequestDTO request = new DepositRequestDTO(new BigDecimal("50.00"), Currency.EUR);
        BalanceDTO eurBalance = new BalanceDTO("EUR", new BigDecimal("150.00")); // 100 + 50
        AccountBalanceResponseDTO response = new AccountBalanceResponseDTO(testAccountId, List.of(eurBalance));
        
        when(accountService.deposit(eq(testAccountId), any(BigDecimal.class), eq(Currency.EUR)))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", testAccountId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(testAccountId)))
                .andExpect(jsonPath("$.balances", hasSize(1)))
                .andExpect(jsonPath("$.balances[0].currency", is("EUR")))
                .andExpect(jsonPath("$.balances[0].amount", is(150.00)));
    }

    @Test
    void deposit_invalidCurrency_returns400() throws Exception {
        String invalidJson = "{\"amount\":50.00,\"currency\":\"INVALID\"}";

        mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", testAccountId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_negativeAmount_returns400() throws Exception {
        DepositRequestDTO request = new DepositRequestDTO(new BigDecimal("-50.00"), Currency.EUR);

        mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", testAccountId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation error")));
    }

    @Test
    void withdraw_success_returnsUpdatedBalances() throws Exception {
        WithdrawRequestDTO request = new WithdrawRequestDTO(new BigDecimal("50.00"), Currency.EUR);
        BalanceDTO eurBalance = new BalanceDTO("EUR", new BigDecimal("50.00")); // 100 - 50
        AccountBalanceResponseDTO response = new AccountBalanceResponseDTO(testAccountId, List.of(eurBalance));
        
        when(accountService.withdraw(eq(testAccountId), any(BigDecimal.class), eq(Currency.EUR)))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts/{accountId}/withdrawals", testAccountId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(testAccountId)))
                .andExpect(jsonPath("$.balances", hasSize(1)))
                .andExpect(jsonPath("$.balances[0].currency", is("EUR")))
                .andExpect(jsonPath("$.balances[0].amount", is(50.00)));
    }

    @Test
    void withdraw_insufficientFunds_returns422() throws Exception {
        WithdrawRequestDTO request = new WithdrawRequestDTO(new BigDecimal("200.00"), Currency.EUR);
        
        when(accountService.withdraw(eq(testAccountId), any(BigDecimal.class), eq(Currency.EUR)))
                .thenThrow(new InsufficientFundsException(
                    testAccountId, 
                    Currency.EUR, 
                    new BigDecimal("200.00"), 
                    new BigDecimal("100.00")));

        mockMvc.perform(post("/api/v1/accounts/{accountId}/withdrawals", testAccountId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("Insufficient funds")));
    }

    @Test
    void withdraw_balanceNotFound_returns400() throws Exception {
        WithdrawRequestDTO request = new WithdrawRequestDTO(new BigDecimal("50.00"), Currency.USD);
        
        when(accountService.withdraw(eq(testAccountId), any(BigDecimal.class), eq(Currency.USD)))
                .thenThrow(new BalanceNotFoundException(testAccountId, Currency.USD));

        mockMvc.perform(post("/api/v1/accounts/{accountId}/withdrawals", testAccountId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Balance not found")));
    }

    @Test
    void exchange_success_returnsUpdatedBalances() throws Exception {
        ExchangeRequestDTO request = new ExchangeRequestDTO(Currency.EUR, Currency.USD, new BigDecimal("50.00"));
        BalanceDTO eurBalance = new BalanceDTO("EUR", new BigDecimal("50.00")); // 100 - 50
        BalanceDTO usdBalance = new BalanceDTO("USD", new BigDecimal("60.00")); // Exchange rate applied
        AccountBalanceResponseDTO response = 
                new AccountBalanceResponseDTO(testAccountId, List.of(eurBalance, usdBalance));
        
        when(accountService.exchange(
                eq(testAccountId), 
                eq(Currency.EUR), 
                eq(Currency.USD), 
                any(BigDecimal.class)))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts/{accountId}/exchanges", testAccountId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(testAccountId)))
                .andExpect(jsonPath("$.balances", hasSize(2)))
                .andExpect(jsonPath("$.balances[0].currency", is("EUR")))
                .andExpect(jsonPath("$.balances[0].amount", is(50.00)))
                .andExpect(jsonPath("$.balances[1].currency", is("USD")))
                .andExpect(jsonPath("$.balances[1].amount", is(60.00)));
    }

    @Test
    void exchange_optimisticLocking_returns409() throws Exception {
        ExchangeRequestDTO request = new ExchangeRequestDTO(Currency.EUR, Currency.USD, new BigDecimal("50.00"));
        
        when(accountService.exchange(
                eq(testAccountId), 
                eq(Currency.EUR), 
                eq(Currency.USD), 
                any(BigDecimal.class)))
            .thenThrow(new OptimisticLockingConflictException("Concurrent modification detected"));

        mockMvc.perform(post("/api/v1/accounts/{accountId}/exchanges", testAccountId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Concurrent modification conflict")));
    }

    @Test
    void exchange_unsupportedCurrencyPair_returns400() throws Exception {
        ExchangeRequestDTO request = new ExchangeRequestDTO(Currency.EUR, Currency.RUB, new BigDecimal("50.00"));
        
        when(accountService.exchange(eq(testAccountId), eq(Currency.EUR), eq(Currency.RUB), any(BigDecimal.class)))
            .thenThrow(new InvalidCurrencyException("Unsupported currency pair: EUR to RUB"));

        mockMvc.perform(post("/api/v1/accounts/{accountId}/exchanges", testAccountId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Invalid currency")));
    }

    @Test
    void exchange_sameCurrency_returns400() throws Exception {
        ExchangeRequestDTO request = new ExchangeRequestDTO(Currency.EUR, Currency.EUR, new BigDecimal("50.00"));
        
        when(accountService.exchange(eq(testAccountId), eq(Currency.EUR), eq(Currency.EUR), any(BigDecimal.class)))
            .thenThrow(new InvalidCurrencyException("Cannot exchange between the same currency: EUR"));

        mockMvc.perform(post("/api/v1/accounts/{accountId}/exchanges", testAccountId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Invalid currency")));
    }

    @Test
    void getTransactionHistory_success_returnsPaginatedTransactions() throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 10);
        
        TransactionResponseDTO tx1 = TransactionResponseDTO.builder()
                .id(1L)
                .accountId(testAccountId)
                .type(TransactionType.DEPOSIT)
                .currency("EUR")
                .amount(new BigDecimal("100.00"))
                .timestamp(Instant.now())
                .build();
                
        TransactionResponseDTO tx2 = TransactionResponseDTO.builder()
                .id(2L)
                .accountId(testAccountId)
                .type(TransactionType.WITHDRAWAL)
                .currency("EUR")
                .amount(new BigDecimal("50.00"))
                .timestamp(Instant.now())
                .build();
                
        Page<TransactionResponseDTO> txPage = new PageImpl<>(
                List.of(tx1, tx2), 
                pageRequest, 
                2);
        
        when(transactionService.getTransactionHistory(eq(testAccountId), eq(0), eq(10)))
            .thenReturn(txPage);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", testAccountId)
                .with(csrf())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].accountId", is(testAccountId)))
                .andExpect(jsonPath("$.content[0].type", is("DEPOSIT")))
                .andExpect(jsonPath("$.content[1].id", is(2)))
                .andExpect(jsonPath("$.content[1].accountId", is(testAccountId)))
                .andExpect(jsonPath("$.content[1].type", is("WITHDRAWAL")));
    }

    @Test
    void getTransactionHistory_accountNotFound_returns404() throws Exception {
        when(transactionService.getTransactionHistory(eq(testAccountId), anyInt(), anyInt()))
            .thenThrow(new AccountNotFoundException(testAccountId));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", testAccountId)
                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Account not found")));
    }
} 