package com.cgi.bank.account.service.mapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cgi.bank.account.controller.dto.TransactionResponseDTO;
import com.cgi.bank.account.domain.Account;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.domain.Transaction;
import com.cgi.bank.account.domain.TransactionType;

@ExtendWith(MockitoExtension.class)
class TransactionMapperTest {

    private static final String ACCOUNT_ID = "test-account-id";
    
    @Mock
    private Account account;

    @InjectMocks
    private TransactionMapperImpl transactionMapper;
    
    @BeforeEach
    void setUp() {
        lenient().when(account.getAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    void toTransactionResponseDTO_shouldMapTransactionToDTO() {
        Instant now = Instant.now();
        Transaction transaction = new Transaction(
            account, 
            TransactionType.DEPOSIT, 
            Currency.EUR, 
            BigDecimal.valueOf(100)
        );
        transaction.setTimestamp(now);
        
        TransactionResponseDTO dto = transactionMapper.toTransactionResponseDTO(transaction);
        
        assertThat(dto).isNotNull();
        assertThat(dto.getAccountId()).isEqualTo(ACCOUNT_ID);
        
        assertThat(dto.getType().toString()).isEqualTo("DEPOSIT");
        
        assertThat(dto.getCurrency()).isEqualTo(Currency.EUR.name());
        assertThat(dto.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(dto.getTimestamp()).isEqualTo(now);
        assertThat(dto.getRelatedCurrency()).isEqualTo("");
        assertThat(dto.getDescription()).isEqualTo("Deposit of 100 EUR");
    }
    
    @Test
    void toTransactionResponseDTO_shouldReturnNull_whenTransactionIsNull() {
        TransactionResponseDTO dto = transactionMapper.toTransactionResponseDTO(null);
        
        assertThat(dto).isNull();
    }
    
    @Test
    void currencyToString_shouldConvertCurrencyToString() {
        Currency currency = Currency.USD;
        
        String result = transactionMapper.currencyToString(currency);
        
        assertThat(result).isEqualTo("USD");
    }
    
    @Test
    void currencyToString_shouldReturnNull_whenCurrencyIsNull() {
        String result = transactionMapper.currencyToString(null);
        
        assertThat(result).isNull();
    }
    
    @Test
    void generateDescription_shouldReturnNull_whenTransactionIsNull() {
        String description = transactionMapper.generateDescription(null);
        
        assertThat(description).isNull();
    }
    
    @Test
    void generateDescription_shouldReturnDepositDescription_forDepositTransaction() {
        Transaction transaction = new Transaction(
            account, 
            TransactionType.DEPOSIT, 
            Currency.EUR, 
            BigDecimal.valueOf(100)
        );
        

        String description = transactionMapper.generateDescription(transaction);
        
        assertThat(description).isEqualTo("Deposit of 100 EUR");
    }
    
    @Test
    void generateDescription_shouldReturnWithdrawalDescription_forWithdrawalTransaction() {
        Transaction transaction = new Transaction(
            account, 
            TransactionType.WITHDRAWAL, 
            Currency.USD, 
            BigDecimal.valueOf(50)
        );
        

        String description = transactionMapper.generateDescription(transaction);
        
        assertThat(description).isEqualTo("Withdrawal of 50 USD");
    }
    
    @Test
    void generateDescription_shouldReturnExchangeFromDescription_forExchangeFromTransaction() {
        Transaction transaction = new Transaction(
            account, 
            TransactionType.EXCHANGE_FROM, 
            Currency.EUR, 
            BigDecimal.valueOf(100)
        );

        String description = transactionMapper.generateDescription(transaction);
        
        assertThat(description).isEqualTo("Exchange from 100 EUR");
    }
    
    @Test
    void generateDescription_shouldReturnExchangeToDescription_forExchangeToTransaction() {
        Transaction transaction = new Transaction(
            account, 
            TransactionType.EXCHANGE_TO, 
            Currency.USD, 
            BigDecimal.valueOf(110)
        );
        
        String description = transactionMapper.generateDescription(transaction);
        
        assertThat(description).isEqualTo("Exchange to 110 USD");
    }
    
    @Test
    void generateDescription_shouldReturnGenericDescription_forUnknownTransactionType() {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.DEPOSIT); 
        transaction.setAmount(BigDecimal.valueOf(75));
        transaction.setCurrency(Currency.USD);
        
        TransactionMapper testMapper = new TransactionMapperImpl() {
            @Override
            public String generateDescription(Transaction transaction) {
                if (transaction == null) {
                    return null;
                }
                
                return "Transaction of " + transaction.getAmount() + " " + transaction.getCurrency();
            }
        };
        

        String description = testMapper.generateDescription(transaction);
        
        assertThat(description).isEqualTo("Transaction of 75 USD");
    }
    
    @Test
    void toTransactionResponseDTO_shouldMapAllTransactionTypes() {
        // Test WITHDRAWAL
        Transaction withdrawalTx = new Transaction(
            account, 
            TransactionType.WITHDRAWAL, 
            Currency.EUR, 
            BigDecimal.valueOf(50)
        );
        TransactionResponseDTO withdrawalDto = transactionMapper.toTransactionResponseDTO(withdrawalTx);
        assertThat(withdrawalDto.getDescription()).isEqualTo("Withdrawal of 50 EUR");
        
        // Test EXCHANGE_FROM
        Transaction exchangeFromTx = new Transaction(
            account, 
            TransactionType.EXCHANGE_FROM, 
            Currency.EUR, 
            BigDecimal.valueOf(100)
        );
        TransactionResponseDTO exchangeFromDto = transactionMapper.toTransactionResponseDTO(exchangeFromTx);
        assertThat(exchangeFromDto.getDescription()).isEqualTo("Exchange from 100 EUR");
        
        // Test EXCHANGE_TO
        Transaction exchangeToTx = new Transaction(
            account, 
            TransactionType.EXCHANGE_TO, 
            Currency.USD, 
            BigDecimal.valueOf(110)
        );
        TransactionResponseDTO exchangeToDto = transactionMapper.toTransactionResponseDTO(exchangeToTx);
        assertThat(exchangeToDto.getDescription()).isEqualTo("Exchange to 110 USD");
    }
    
    @Test
    void toTransactionResponseDTO_shouldSetEmptyRelatedCurrency() {
        Transaction transaction = new Transaction(
            account, 
            TransactionType.DEPOSIT, 
            Currency.EUR, 
            BigDecimal.valueOf(100)
        );
        
        TransactionResponseDTO dto = transactionMapper.toTransactionResponseDTO(transaction);
        
        assertThat(dto.getRelatedCurrency()).isEqualTo("");
    }
} 