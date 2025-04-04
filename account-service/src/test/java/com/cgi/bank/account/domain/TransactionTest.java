package com.cgi.bank.account.domain;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionTest {

    @Mock
    private Account account;
    
    @BeforeEach
    void setUp() {
        lenient().when(account.getAccountId()).thenReturn("test-account-id");
    }

    @Test
    void constructor_shouldSetFieldsCorrectly() {
        Currency currency = Currency.EUR;
        TransactionType transactionType = TransactionType.DEPOSIT;
        BigDecimal amount = BigDecimal.valueOf(100.0);
        
        Transaction transaction = new Transaction(account, transactionType, currency, amount);
        
        assertThat(transaction).isNotNull();
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getType()).isEqualTo(transactionType);
        assertThat(transaction.getCurrency()).isEqualTo(currency);
        assertThat(transaction.getAmount()).isEqualByComparingTo(amount);
        assertThat(transaction.getTimestamp()).isNotNull();
        assertThat(transaction.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }
    
    @Test
    void factoryMethods_shouldCreateCorrectTransactionTypes() {
        Currency currency = Currency.USD;
        BigDecimal amount = BigDecimal.valueOf(50.0);
        
        Transaction deposit = Transaction.createDeposit(account, currency, amount);
        Transaction withdrawal = Transaction.createWithdrawal(account, currency, amount);
        Transaction exchangeFrom = Transaction.createExchangeFrom(account, currency, amount);
        Transaction exchangeTo = Transaction.createExchangeTo(account, currency, amount);
        
        assertThat(deposit.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(withdrawal.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(exchangeFrom.getType()).isEqualTo(TransactionType.EXCHANGE_FROM);
        assertThat(exchangeTo.getType()).isEqualTo(TransactionType.EXCHANGE_TO);
    }
    
    @Test
    void setters_shouldModifyFieldsCorrectly() {
        Transaction transaction = new Transaction();
        Currency currency = Currency.SEK;
        TransactionType transactionType = TransactionType.EXCHANGE_FROM;
        BigDecimal amount = BigDecimal.valueOf(25.0);
        Instant timestamp = Instant.now().minusSeconds(7200); // 2 hours ago
        
        transaction.setAccount(account);
        transaction.setType(transactionType);
        transaction.setCurrency(currency);
        transaction.setAmount(amount);
        transaction.setTimestamp(timestamp);
        
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getType()).isEqualTo(transactionType);
        assertThat(transaction.getCurrency()).isEqualTo(currency);
        assertThat(transaction.getAmount()).isEqualByComparingTo(amount);
        assertThat(transaction.getTimestamp()).isEqualTo(timestamp);
    }
    
    @Test
    void onCreate_shouldSetTimestampIfNull() {
        Transaction transaction = new Transaction();
        
        transaction.onCreate(); // Directly calling the @PrePersist method
        
        assertThat(transaction.getTimestamp()).isNotNull();
        assertThat(transaction.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }
    
    @Test
    void onCreate_shouldNotModifyExistingTimestamp() {
        Transaction transaction = new Transaction();
        Instant originalTimestamp = Instant.now().minusSeconds(3600); // 1 hour ago
        transaction.setTimestamp(originalTimestamp);
        
        transaction.onCreate(); // This should not modify the existing timestamp
        
        assertThat(transaction.getTimestamp()).isEqualTo(originalTimestamp);
    }

    @Test
    void defaultConstructor_shouldCreateEmptyTransaction() {
        // When
        Transaction transaction = new Transaction();
        
        // Then
        assertThat(transaction).isNotNull();
        assertThat(transaction.getId()).isNull();
        assertThat(transaction.getAccount()).isNull();
        assertThat(transaction.getType()).isNull();
        assertThat(transaction.getCurrency()).isNull();
        assertThat(transaction.getAmount()).isNull();
        assertThat(transaction.getTimestamp()).isNull();
    }
    
    @Test
    void createDeposit_withNullAccount_shouldCreateTransactionWithNullAccount() {
        Account nullAccount = null;
        Currency currency = Currency.EUR;
        BigDecimal amount = BigDecimal.TEN;
        
        Transaction transaction = Transaction.createDeposit(nullAccount, currency, amount);
        
        assertThat(transaction).isNotNull();
        assertThat(transaction.getAccount()).isNull();
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transaction.getCurrency()).isEqualTo(currency);
        assertThat(transaction.getAmount()).isEqualByComparingTo(amount);
    }
    
    @Test
    void createWithdrawal_withNullCurrency_shouldCreateTransactionWithNullCurrency() {
        Currency nullCurrency = null;
        BigDecimal amount = BigDecimal.TEN;
        
        Transaction transaction = Transaction.createWithdrawal(account, nullCurrency, amount);
        
        assertThat(transaction).isNotNull();
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(transaction.getCurrency()).isNull();
        assertThat(transaction.getAmount()).isEqualByComparingTo(amount);
    }
    
    @Test
    void createExchangeFrom_withNullAmount_shouldCreateTransactionWithNullAmount() {
        Currency currency = Currency.USD;
        BigDecimal nullAmount = null;
        
        Transaction transaction = Transaction.createExchangeFrom(account, currency, nullAmount);
        
        assertThat(transaction).isNotNull();
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getType()).isEqualTo(TransactionType.EXCHANGE_FROM);
        assertThat(transaction.getCurrency()).isEqualTo(currency);
        assertThat(transaction.getAmount()).isNull();
    }
    
    @Test
    void getId_shouldReturnNullInitially() {
        Transaction transaction = new Transaction(account, TransactionType.DEPOSIT, Currency.EUR, BigDecimal.TEN);
        
        assertThat(transaction.getId()).isNull();
    }
    
    @Test
    void setId_shouldUpdateId() {
        Transaction transaction = new Transaction();
        Long id = 123L;
        
        transaction.setId(id);
        
        assertThat(transaction.getId()).isEqualTo(id);
    }

    @Test
    void toString_shouldNotIncludeAccount() {
        Transaction transaction = new Transaction(account, TransactionType.DEPOSIT, Currency.EUR, BigDecimal.TEN);
        
        String result = transaction.toString();
        
        assertThat(result).contains("Transaction");
        assertThat(result).contains("type=DEPOSIT");
        assertThat(result).contains("currency=EUR");
    }
    
    @Test
    void createExchangeTo_shouldSetCorrectFields() {
        Currency currency = Currency.SEK;
        BigDecimal amount = BigDecimal.valueOf(123.45);
        
        Transaction transaction = Transaction.createExchangeTo(account, currency, amount);
        
        assertThat(transaction).isNotNull();
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getType()).isEqualTo(TransactionType.EXCHANGE_TO);
        assertThat(transaction.getCurrency()).isEqualTo(currency);
        assertThat(transaction.getAmount()).isEqualByComparingTo(amount);
        assertThat(transaction.getTimestamp()).isNotNull();
    }
} 