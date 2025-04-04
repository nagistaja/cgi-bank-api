package com.cgi.bank.account.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cgi.bank.account.controller.dto.AccountBalanceResponseDTO;
import com.cgi.bank.account.controller.dto.BalanceDTO;
import com.cgi.bank.account.domain.Account;
import com.cgi.bank.account.domain.Balance;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.domain.Transaction;
import com.cgi.bank.account.exception.AccountNotFoundException;
import com.cgi.bank.account.exception.BalanceNotFoundException;
import com.cgi.bank.account.exception.InsufficientFundsException;
import com.cgi.bank.account.integration.NotificationClient;
import com.cgi.bank.account.repository.AccountRepository;
import com.cgi.bank.account.repository.TransactionRepository;
import com.cgi.bank.account.service.impl.AccountServiceImpl;
import com.cgi.bank.account.service.mapper.BalanceMapper;

/**
 * Unit tests for the AccountService implementation.
 * Uses Mockito to mock dependencies and focus on testing service logic in isolation.
 */
@DisplayName("Account Service Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CurrencyExchangeService currencyExchangeService;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private BalanceMapper balanceMapper;

    private AccountServiceImpl accountService;

    private static final String TEST_ACCOUNT_ID = "account123";
    private Account testAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        accountService = new AccountServiceImpl(
            accountRepository,
            transactionRepository,
            currencyExchangeService,
            notificationClient,
            balanceMapper
        );
        
        testAccount = new Account();
        testAccount.setAccountId(TEST_ACCOUNT_ID);

        Balance eurBalance = new Balance(testAccount, Currency.EUR, new BigDecimal("100.00"));
        Balance usdBalance = new Balance(testAccount, Currency.USD, new BigDecimal("150.00"));

        Map<Currency, Balance> balances = new HashMap<>();
        balances.put(Currency.EUR, eurBalance);
        balances.put(Currency.USD, usdBalance);
        testAccount.setBalances(balances);
    }

    @Nested
    @DisplayName("Account balance retrieval")
    class GetAccountBalances {
        @Test
        @DisplayName("Returns balances when account exists")
        void returnsBalancesWhenAccountExists() {
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

            AccountBalanceResponseDTO result = accountService.getAccountBalances(TEST_ACCOUNT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
            assertThat(result.getBalances()).hasSize(2);
            
            // Verify balances contain both currencies
            List<String> currencies = result.getBalances().stream()
                    .map(BalanceDTO::getCurrency)
                    .toList();
            assertThat(currencies).contains("EUR", "USD");
            
            // Verify EUR balance amount
            Optional<BalanceDTO> eurBalance = result.getBalances().stream()
                    .filter(b -> b.getCurrency().equals("EUR"))
                    .findFirst();
            assertThat(eurBalance).isPresent();
            assertThat(eurBalance.get().getAmount())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(new BigDecimal("100.00"));
            
            verify(accountRepository).findByIdWithBalances(TEST_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Throws AccountNotFoundException when account doesn't exist")
        void throwsExceptionWhenAccountDoesNotExist() {
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());
            
            assertThatThrownBy(() -> accountService.getAccountBalances(TEST_ACCOUNT_ID))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(TEST_ACCOUNT_ID);
            
            verify(accountRepository).findByIdWithBalances(TEST_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("Deposit operations")
    class DepositOperations {
        @Test
        @DisplayName("Deposits amount into account when valid")
        void depositsWhenValid() {
            BigDecimal depositAmount = new BigDecimal("50.00");
            Currency depositCurrency = Currency.EUR;
            
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());
            when(notificationClient.sendDepositNotification(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));
            
            AccountBalanceResponseDTO result = accountService.deposit(TEST_ACCOUNT_ID, depositAmount, depositCurrency);

            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
            
            verify(accountRepository).findByIdWithBalances(TEST_ACCOUNT_ID);
            verify(accountRepository).save(any(Account.class));
            verify(transactionRepository).save(any(Transaction.class));
            verify(notificationClient).sendDepositNotification(
                    eq(TEST_ACCOUNT_ID), 
                    eq(depositAmount.toString()), 
                    eq(depositCurrency.name()));
        }

        @Test
        @DisplayName("Negative amount throws IllegalArgumentException")
        void negativeAmountThrowsIllegalArgumentException() {
            BigDecimal depositAmount = new BigDecimal("-50.00");
            Currency depositCurrency = Currency.EUR;
            
            assertThatThrownBy(() -> accountService.deposit(TEST_ACCOUNT_ID, depositAmount, depositCurrency))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
            
            verifyNoInteractions(accountRepository, notificationClient);
        }

        @Test
        @DisplayName("Non-existent account throws AccountNotFoundException")
        void nonExistentAccountThrowsAccountNotFoundException() {
            BigDecimal depositAmount = new BigDecimal("50.00");
            Currency depositCurrency = Currency.EUR;
            
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());
            
            assertThatThrownBy(() -> accountService.deposit(TEST_ACCOUNT_ID, depositAmount, depositCurrency))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(TEST_ACCOUNT_ID);
            
            verify(accountRepository).findByIdWithBalances(TEST_ACCOUNT_ID);
            verifyNoInteractions(notificationClient);
            verifyNoInteractions(transactionRepository);
        }
    }

    @Nested
    @DisplayName("Withdrawal operations")
    class WithdrawalOperations {
        @Test
        @DisplayName("Successful withdrawal subtracts balance")
        void successfulWithdrawalSubtractsBalance() {
            BigDecimal withdrawAmount = new BigDecimal("50.00");
            Currency withdrawCurrency = Currency.EUR;
            
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());
            
            AccountBalanceResponseDTO result = accountService.withdraw(
                TEST_ACCOUNT_ID, 
                withdrawAmount, 
                withdrawCurrency
            );
            
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
            
            verify(accountRepository).findByIdWithBalances(TEST_ACCOUNT_ID);
            verify(accountRepository).save(any(Account.class));
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Non-existent account throws AccountNotFoundException")
        void nonExistentAccountThrowsAccountNotFoundException() {
            BigDecimal withdrawAmount = new BigDecimal("50.00");
            Currency withdrawCurrency = Currency.EUR;
            
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());
            
            assertThatThrownBy(() -> accountService.withdraw(TEST_ACCOUNT_ID, withdrawAmount, withdrawCurrency))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(TEST_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Negative amount throws IllegalArgumentException")
        void negativeAmountThrowsIllegalArgumentException() {
            BigDecimal withdrawAmount = new BigDecimal("-50.00");
            Currency withdrawCurrency = Currency.EUR;
            
            assertThatThrownBy(() -> accountService.withdraw(TEST_ACCOUNT_ID, withdrawAmount, withdrawCurrency))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
            
            verifyNoInteractions(accountRepository);
        }

        @Test
        @DisplayName("Non-existent balance throws BalanceNotFoundException")
        void nonExistentBalanceThrowsBalanceNotFoundException() {
            BigDecimal withdrawAmount = new BigDecimal("50.00");
            Currency withdrawCurrency = Currency.SEK; // Account has no SEK balance
            
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            
            assertThatThrownBy(() -> accountService.withdraw(TEST_ACCOUNT_ID, withdrawAmount, withdrawCurrency))
                    .isInstanceOf(BalanceNotFoundException.class)
                    .hasMessageContaining(TEST_ACCOUNT_ID)
                    .hasMessageContaining(withdrawCurrency.name());
        }

        @Test
        @DisplayName("Insufficient funds throws InsufficientFundsException")
        void insufficientFundsThrowsInsufficientFundsException() {
            BigDecimal withdrawAmount = new BigDecimal("150.00"); // More than available EUR balance (100.00)
            Currency withdrawCurrency = Currency.EUR;
            
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            
            assertThatThrownBy(() -> accountService.withdraw(TEST_ACCOUNT_ID, withdrawAmount, withdrawCurrency))
                    .isInstanceOf(InsufficientFundsException.class);
        }
    }

    @Nested
    @DisplayName("Exchange operations")
    class ExchangeOperations {
        @Test
        @DisplayName("Successful exchange converts balances")
        void successfulExchangeConvertsBalances() {
            BigDecimal exchangeAmount = new BigDecimal("50.00");
            Currency fromCurrency = Currency.EUR;
            Currency toCurrency = Currency.USD;
            BigDecimal exchangedAmount = new BigDecimal("55.00"); // Mocked exchange rate result
            
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(currencyExchangeService.calculateExchange(fromCurrency, toCurrency, exchangeAmount))
                    .thenReturn(exchangedAmount);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());
            
            AccountBalanceResponseDTO result = accountService.exchange(
                    TEST_ACCOUNT_ID, fromCurrency, toCurrency, exchangeAmount);

            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
            
            verify(accountRepository).findByIdWithBalances(TEST_ACCOUNT_ID);
            verify(currencyExchangeService).calculateExchange(fromCurrency, toCurrency, exchangeAmount);
            verify(accountRepository).save(any(Account.class));
            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }
        
        @Test
        @DisplayName("Non-existent account throws AccountNotFoundException")
        void nonExistentAccountThrowsAccountNotFoundException() {
            BigDecimal exchangeAmount = new BigDecimal("50.00");
            Currency fromCurrency = Currency.EUR;
            Currency toCurrency = Currency.USD;
            
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());
            
            assertThatThrownBy(() -> accountService.exchange(TEST_ACCOUNT_ID, fromCurrency, toCurrency, exchangeAmount))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(TEST_ACCOUNT_ID);
            
            verify(accountRepository).findByIdWithBalances(TEST_ACCOUNT_ID);
            verifyNoInteractions(currencyExchangeService);
        }
        
        @Test
        @DisplayName("Non-existent from-balance throws BalanceNotFoundException")
        void nonExistentFromBalanceThrowsBalanceNotFoundException() {
            final BigDecimal exchangeAmount = new BigDecimal("50.00");
            final Currency fromCurrency = Currency.SEK; // Not in our test account
            final Currency toCurrency = Currency.USD;
            
            Account accountWithoutSek = new Account();
            accountWithoutSek.setAccountId(TEST_ACCOUNT_ID);
            Balance usdBalance = new Balance(accountWithoutSek, Currency.USD, new BigDecimal("150.00"));
            
            Map<Currency, Balance> balances = new HashMap<>();
            balances.put(Currency.USD, usdBalance);
            accountWithoutSek.setBalances(balances);
            
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.of(accountWithoutSek));
            
            assertThatThrownBy(() -> accountService.exchange(TEST_ACCOUNT_ID, fromCurrency, toCurrency, exchangeAmount))
                    .isInstanceOf(BalanceNotFoundException.class)
                    .hasMessageContaining(TEST_ACCOUNT_ID)
                    .hasMessageContaining(fromCurrency.name());
            
            verify(accountRepository).findByIdWithBalances(TEST_ACCOUNT_ID);
            verifyNoInteractions(currencyExchangeService);
        }
        
        @Test
        @DisplayName("Insufficient funds throws InsufficientFundsException")
        void insufficientFundsThrowsInsufficientFundsException() {
            BigDecimal exchangeAmount = new BigDecimal("150.00"); // More than the 100 EUR available
            Currency fromCurrency = Currency.EUR;
            Currency toCurrency = Currency.USD;
            
            when(accountRepository.findByIdWithBalances(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            
            assertThatThrownBy(() -> accountService.exchange(TEST_ACCOUNT_ID, fromCurrency, toCurrency, exchangeAmount))
                    .isInstanceOf(InsufficientFundsException.class);
            
            verify(accountRepository).findByIdWithBalances(TEST_ACCOUNT_ID);
        }
        
        @Test
        @DisplayName("Negative amount throws IllegalArgumentException")
        void negativeAmountThrowsIllegalArgumentException() {
            BigDecimal exchangeAmount = new BigDecimal("-50.00");
            Currency fromCurrency = Currency.EUR;
            Currency toCurrency = Currency.USD;
            
            assertThatThrownBy(() -> accountService.exchange(TEST_ACCOUNT_ID, fromCurrency, toCurrency, exchangeAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
            
            verifyNoInteractions(accountRepository, currencyExchangeService);
        }
        
        @Test
        @DisplayName("Same currency throws IllegalArgumentException")
        void sameCurrencyThrowsIllegalArgumentException() {
            BigDecimal exchangeAmount = new BigDecimal("50.00");
            Currency currency = Currency.EUR;
            
            assertThatThrownBy(() -> accountService.exchange(TEST_ACCOUNT_ID, currency, currency, exchangeAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different");
            
            verifyNoInteractions(accountRepository, currencyExchangeService);
        }
    }
} 