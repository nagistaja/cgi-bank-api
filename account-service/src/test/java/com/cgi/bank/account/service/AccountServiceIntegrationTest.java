package com.cgi.bank.account.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.cgi.bank.account.config.TestSecurityConfig;
import com.cgi.bank.account.controller.dto.AccountBalanceResponseDTO;
import com.cgi.bank.account.controller.dto.BalanceDTO;
import com.cgi.bank.account.domain.Account;
import com.cgi.bank.account.domain.Balance;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.exception.AccountNotFoundException;
import com.cgi.bank.account.exception.BalanceNotFoundException;
import com.cgi.bank.account.exception.InsufficientFundsException;
import com.cgi.bank.account.integration.AbstractIntegrationTest;
import com.cgi.bank.account.integration.NotificationClient;
import com.cgi.bank.account.repository.AccountRepository;

import jakarta.persistence.EntityManager;

/**
 * Integration tests for the AccountService.
 * Tests the service with a real (Testcontainer) database.
 * Using transactional tests for better isolation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({TestSecurityConfig.class})
@Tag("integration")
class AccountServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoBean
    private NotificationClient notificationClient;
    
    @MockitoBean
    private CurrencyExchangeService currencyExchangeService;
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        Mockito.reset(notificationClient);
        when(currencyExchangeService.calculateExchange(eq(Currency.EUR), eq(Currency.USD), any(BigDecimal.class)))
            .thenAnswer(invocation -> ((BigDecimal) invocation.getArgument(2)).multiply(new BigDecimal("1.1")));
        when(currencyExchangeService.calculateExchange(eq(Currency.USD), eq(Currency.EUR), any(BigDecimal.class)))
            .thenAnswer(invocation -> ((BigDecimal) invocation.getArgument(2)).multiply(new BigDecimal("0.9")));
    }
    
    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    private String createTestAccount() {
        AccountBalanceResponseDTO responseDTO = accountService.createAccount();
        return responseDTO.getAccountId();
    }
    
    private String createTestAccountWithBalance(Currency currency, BigDecimal amount) {
        AccountBalanceResponseDTO responseDTO = accountService.createAccount();
        String accountId = responseDTO.getAccountId();
        accountService.deposit(accountId, amount, currency);
        return accountId;
    }

    @Test
    void getAccountBalances_nonExistentAccount_throwsAccountNotFoundException() {
        assertThatThrownBy(() -> accountService.getAccountBalances("non-existent-id"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("non-existent-id");
    }

    @Test
    void deposit_newAccount_createsAccountWithBalance() {
        String accountId = createTestAccount();
        BigDecimal depositAmount = new BigDecimal("100.00");
        Currency currency = Currency.EUR;
        
        AccountBalanceResponseDTO response = accountService.deposit(accountId, depositAmount, currency);
        
        assertThat(response).isNotNull();
        assertThat(response.getAccountId()).isEqualTo(accountId);
        assertThat(response.getBalances()).hasSize(1);
        
        BalanceDTO balanceDTO = response.getBalances().get(0);
        assertThat(balanceDTO.getAmount()).isEqualByComparingTo(depositAmount);
        assertThat(balanceDTO.getCurrency().toString()).isEqualTo(currency.toString());
        
        entityManager.clear(); // Clear persistence context before querying
        Account savedAccount = accountRepository.findByIdWithBalances(accountId).orElseThrow();
        assertThat(savedAccount.getBalances()).hasSize(1);
        
        Balance savedBalance = savedAccount.getBalances().get(currency);
        assertThat(savedBalance).isNotNull();
        assertThat(savedBalance.getAmount()).isEqualByComparingTo(depositAmount);
        
        verify(notificationClient).sendDepositNotification(
                accountId, 
                depositAmount.toString(),
                currency.name());
    }

    @Test
    void deposit_existingAccount_addsToExistingBalance() {
        BigDecimal initialAmount = new BigDecimal("50.00");
        BigDecimal depositAmount = new BigDecimal("30.00");
        final BigDecimal expectedFinalAmount = initialAmount.add(depositAmount);
        Currency currency = Currency.EUR;
        
        String accountId = createTestAccountWithBalance(currency, initialAmount);
        entityManager.clear();
        
        AccountBalanceResponseDTO response = accountService.deposit(accountId, depositAmount, currency);
        
        assertThat(response).isNotNull();
        assertThat(response.getAccountId()).isEqualTo(accountId);
        assertThat(response.getBalances()).hasSize(1);
        
        BalanceDTO balanceDTO = response.getBalances().get(0);
        assertThat(balanceDTO.getAmount()).isEqualByComparingTo(expectedFinalAmount);
        assertThat(balanceDTO.getCurrency().toString()).isEqualTo(currency.toString());
        
        entityManager.clear();
        Account savedAccount = accountRepository.findByIdWithBalances(accountId).orElseThrow();
        assertThat(savedAccount.getBalances()).hasSize(1);
        
        Balance savedBalance = savedAccount.getBalances().get(currency);
        assertThat(savedBalance).isNotNull();
        assertThat(savedBalance.getAmount()).isEqualByComparingTo(expectedFinalAmount);
        
        verify(notificationClient).sendDepositNotification(
                accountId, 
                depositAmount.toString(),
                currency.name());
    }

    @Test
    void withdraw_success_reducesBalance() {
        BigDecimal initialAmount = new BigDecimal("100.00");
        BigDecimal withdrawAmount = new BigDecimal("30.00");
        final BigDecimal expectedFinalAmount = initialAmount.subtract(withdrawAmount);
        Currency currency = Currency.EUR;
        
        String accountId = createTestAccountWithBalance(currency, initialAmount);
        entityManager.clear();
        
        AccountBalanceResponseDTO response = accountService.withdraw(accountId, withdrawAmount, currency);
        
        assertThat(response).isNotNull();
        assertThat(response.getAccountId()).isEqualTo(accountId);
        assertThat(response.getBalances()).hasSize(1);
        
        BalanceDTO balanceDTO = response.getBalances().get(0);
        assertThat(balanceDTO.getAmount()).isEqualByComparingTo(expectedFinalAmount);
        assertThat(balanceDTO.getCurrency().toString()).isEqualTo(currency.toString());
        
        entityManager.clear();
        Account savedAccount = accountRepository.findByIdWithBalances(accountId).orElseThrow();
        assertThat(savedAccount.getBalances()).hasSize(1);
        
        Balance savedBalance = savedAccount.getBalances().get(currency);
        assertThat(savedBalance).isNotNull();
        assertThat(savedBalance.getAmount()).isEqualByComparingTo(expectedFinalAmount);
    }

    @Test
    void withdraw_insufficientFunds_throwsInsufficientFundsException() {
        BigDecimal initialAmount = new BigDecimal("20.00");
        BigDecimal withdrawAmount = new BigDecimal("50.00"); // More than available
        Currency currency = Currency.EUR;
        
        String accountId = createTestAccountWithBalance(currency, initialAmount);
        entityManager.clear();
        
        assertThatThrownBy(() -> accountService.withdraw(accountId, withdrawAmount, currency))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void withdraw_nonExistentBalance_throwsBalanceNotFoundException() {
        String accountId = createTestAccount();
        BigDecimal withdrawAmount = new BigDecimal("30.00");
        Currency currency = Currency.EUR; // Account doesn't have EUR balance
        
        assertThatThrownBy(() -> accountService.withdraw(accountId, withdrawAmount, currency))
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("Balance not found");
    }

    @Test
    void exchange_success_convertsBalances() {
        BigDecimal initialAmount = new BigDecimal("100.00");
        Currency sourceCurrency = Currency.EUR;
        Currency targetCurrency = Currency.USD;
        
        final BigDecimal expectedTargetAmount = initialAmount.multiply(new BigDecimal("1.1"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        
        String accountId = createTestAccountWithBalance(sourceCurrency, initialAmount);
        entityManager.clear();
        
        AccountBalanceResponseDTO response = accountService.exchange(
                accountId, sourceCurrency, targetCurrency, initialAmount);
        
        assertThat(response).isNotNull();
        assertThat(response.getAccountId()).isEqualTo(accountId);
        assertThat(response.getBalances()).hasSize(2);
        
        Map<String, BalanceDTO> balanceMap = new HashMap<>();
        for (BalanceDTO dto : response.getBalances()) {
            balanceMap.put(dto.getCurrency().toString(), dto);
        }
        
        BalanceDTO sourceBalance = balanceMap.get(sourceCurrency.toString());
        BalanceDTO targetBalance = balanceMap.get(targetCurrency.toString());
        
        assertThat(sourceBalance).isNotNull();
        assertThat(targetBalance).isNotNull();
        
        assertThat(sourceBalance.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(targetBalance.getAmount()).isEqualByComparingTo(expectedTargetAmount);
        
        verify(currencyExchangeService).calculateExchange(sourceCurrency, targetCurrency, initialAmount);
    }

    @Test
    void exchange_insufficientFunds_throwsInsufficientFundsException() {
        BigDecimal initialAmount = new BigDecimal("50.00");
        BigDecimal exchangeAmount = new BigDecimal("100.00"); // More than available
        Currency sourceCurrency = Currency.EUR;
        Currency targetCurrency = Currency.USD;
        
        String accountId = createTestAccountWithBalance(sourceCurrency, initialAmount);
        entityManager.clear();
        
        assertThatThrownBy(() -> 
            accountService.exchange(accountId, sourceCurrency, targetCurrency, exchangeAmount))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Nested
    class OptimisticLockingTests {
        @Test
        void withdraw_concurrentModification_throwsOptimisticLockingConflictException() {
            String accountId = createTestAccountWithBalance(Currency.EUR, new BigDecimal("100.00"));
            
            entityManager.clear();
            
            Account staleAccount = accountRepository.findByIdWithBalances(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
            
            // Update the account in a separate transaction
            new TransactionTemplate(transactionManager).execute(status -> {
                Account freshAccount = accountRepository.findByIdWithBalances(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));
                Balance eurBalance = freshAccount.getBalance(Currency.EUR).orElseThrow();
                eurBalance.subtractAmount(new BigDecimal("10.00"));
                accountRepository.saveAndFlush(freshAccount);
                return null;
            });
            
            // Try to update with stale version - should throw exception
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            assertThatThrownBy(() -> {
                template.execute(status -> {
                    Balance staleBalance = staleAccount.getBalance(Currency.EUR).orElseThrow();
                    staleBalance.subtractAmount(new BigDecimal("20.00"));
                    accountRepository.saveAndFlush(staleAccount);
                    return null;
                });
            })
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }
        
        @Test
        void exchange_concurrentModification_throwsOptimisticLockingConflictException() {
            String accountId = createTestAccountWithBalance(Currency.EUR, new BigDecimal("100.00"));
            accountService.deposit(accountId, new BigDecimal("50.00"), Currency.USD);
            
            entityManager.clear();
            
            // Get a stale version of the account
            Account staleAccount = accountRepository.findByIdWithBalances(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
            
            // Update the account in a separate transaction
            new TransactionTemplate(transactionManager).execute(status -> {
                Account freshAccount = accountRepository.findByIdWithBalances(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));
                Balance eurBalance = freshAccount.getBalance(Currency.EUR).orElseThrow();
                eurBalance.subtractAmount(new BigDecimal("10.00"));
                accountRepository.saveAndFlush(freshAccount);
                return null;
            });
            
            // Try to update with stale version - should throw exception
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            assertThatThrownBy(() -> {
                template.execute(status -> {
                    Balance eurBalance = staleAccount.getBalance(Currency.EUR).orElseThrow();
                    Balance usdBalance = staleAccount.getBalance(Currency.USD).orElseThrow();
                    BigDecimal amount = new BigDecimal("20.00");
                    eurBalance.subtractAmount(amount);
                    usdBalance.addAmount(amount.multiply(new BigDecimal("1.1")));
                    accountRepository.saveAndFlush(staleAccount);
                    return null;
                });
            })
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }
    }
} 