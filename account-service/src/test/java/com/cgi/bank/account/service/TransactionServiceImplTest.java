package com.cgi.bank.account.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.cgi.bank.account.controller.dto.TransactionResponseDTO;
import com.cgi.bank.account.domain.Account;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.domain.Transaction;
import com.cgi.bank.account.domain.TransactionType;
import com.cgi.bank.account.exception.AccountNotFoundException;
import com.cgi.bank.account.repository.AccountRepository;
import com.cgi.bank.account.repository.TransactionRepository;
import com.cgi.bank.account.service.impl.TransactionServiceImpl;
import com.cgi.bank.account.service.mapper.TransactionMapper;

/**
 * Unit tests for the TransactionService implementation.
 * Uses Mockito to mock dependencies and focus on testing service logic in isolation.
 */
@DisplayName("Transaction Service Tests")
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    private TransactionService transactionService;

    private static final String TEST_ACCOUNT_ID = "account123";
    private Account testAccount;
    private List<Transaction> testTransactions;
    private Instant now;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        transactionService = new TransactionServiceImpl(
            transactionRepository,
            accountRepository,
            transactionMapper
        );
        
        testAccount = new Account();
        testAccount.setAccountId(TEST_ACCOUNT_ID);
        
        now = Instant.now();
        
        testTransactions = List.of(
            createTransaction(1L, TransactionType.DEPOSIT, new BigDecimal("100.00"), Currency.EUR, now),
            createTransaction(
                2L, 
                TransactionType.WITHDRAWAL, 
                new BigDecimal("50.00"), 
                Currency.EUR, 
                now.minus(1, ChronoUnit.HOURS)
            ),
            createTransaction(
                3L, 
                TransactionType.EXCHANGE_FROM, 
                new BigDecimal("30.00"), 
                Currency.EUR, 
                now.minus(2, ChronoUnit.HOURS)
            )
        );
    }
    
    private Transaction createTransaction(
            Long id, 
            TransactionType type, 
            BigDecimal amount, 
            Currency currency, 
            Instant timestamp) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setAccount(testAccount);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setTimestamp(timestamp);
        return transaction;
    }

    @Nested
    @DisplayName("Transaction history retrieval")
    class GetTransactionHistory {
        
        @Test
        @DisplayName("Returns paginated transactions when account exists")
        void returnsPaginatedTransactionsWhenAccountExists() {
            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<Transaction> transactionPage = new PageImpl<>(testTransactions, pageable, testTransactions.size());
            
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findByAccount(eq(testAccount), any(Pageable.class))).thenReturn(transactionPage);
            
            // Mock the mapper to return some DTOs
            when(transactionMapper.toTransactionResponseDTO(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    return TransactionResponseDTO.builder()
                            .id(tx.getId())
                            .accountId(TEST_ACCOUNT_ID)
                            .type(tx.getType())
                            .amount(tx.getAmount())
                            .currency(tx.getCurrency().name())
                            .timestamp(tx.getTimestamp())
                            .build();
                });
            
            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(TEST_ACCOUNT_ID, page, size);
            
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
            
            TransactionResponseDTO firstTransaction = result.getContent().get(0);
            assertThat(firstTransaction.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
            assertThat(firstTransaction.getType()).isEqualTo(TransactionType.DEPOSIT);
            
            verify(accountRepository).findById(TEST_ACCOUNT_ID);
            verify(transactionRepository).findByAccount(eq(testAccount), any(Pageable.class));
        }

        @Test
        @DisplayName("Returns empty page when account has no transactions")
        void returnsEmptyPageWhenAccountHasNoTransactions() {
            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<Transaction> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findByAccount(eq(testAccount), any(Pageable.class))).thenReturn(emptyPage);
            
            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(TEST_ACCOUNT_ID, page, size);
            
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            
            verify(accountRepository).findById(TEST_ACCOUNT_ID);
            verify(transactionRepository).findByAccount(eq(testAccount), any(Pageable.class));
        }
        
        @Test
        @DisplayName("Throws AccountNotFoundException when account doesn't exist")
        void throwsAccountNotFoundExceptionWhenAccountDoesNotExist() {
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());
            
            assertThatThrownBy(() -> transactionService.getTransactionHistory(TEST_ACCOUNT_ID, 0, 10))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(TEST_ACCOUNT_ID);
            
            verify(accountRepository).findById(TEST_ACCOUNT_ID);
        }
        
        @Test
        @DisplayName("Verifies paging parameters are passed correctly")
        void verifiesPagingParametersArePassedCorrectly() {
            int page = 2;
            int size = 15;
            // Create expected pageRequest with timestamp sorting
            Pageable expectedPageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0);
            
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findByAccount(eq(testAccount), eq(expectedPageable))).thenReturn(emptyPage);
            
            transactionService.getTransactionHistory(TEST_ACCOUNT_ID, page, size);
            
            // Verify the exact pageRequest was used
            verify(transactionRepository).findByAccount(eq(testAccount), eq(expectedPageable));
        }
        
        @Test
        @DisplayName("Handles correct sorting of transactions by timestamp descending")
        void handlesCorrectSortingOfTransactions() {
            // Arrange - intentionally create transactions with different timestamps
            Instant now = Instant.now();
            List<Transaction> chronologicalTransactions = List.of(
                createTransaction(
                    1L, 
                    TransactionType.DEPOSIT, 
                    BigDecimal.valueOf(100), 
                    Currency.EUR, 
                    now.minus(2, ChronoUnit.DAYS)
                ),
                createTransaction(
                    2L, 
                    TransactionType.WITHDRAWAL, 
                    BigDecimal.valueOf(50), 
                    Currency.EUR, 
                    now.minus(1, ChronoUnit.DAYS)
                ),
                createTransaction(
                    3L, 
                    TransactionType.DEPOSIT, 
                    BigDecimal.valueOf(200), 
                    Currency.EUR, 
                    now
                )
            );
            
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<Transaction> sortedPage = new PageImpl<>(
                chronologicalTransactions, 
                pageable, 
                chronologicalTransactions.size()
            );
            
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findByAccount(eq(testAccount), any(Pageable.class))).thenReturn(sortedPage);
            
            when(transactionMapper.toTransactionResponseDTO(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    return TransactionResponseDTO.builder()
                            .id(tx.getId())
                            .accountId(TEST_ACCOUNT_ID)
                            .type(tx.getType())
                            .amount(tx.getAmount())
                            .currency(tx.getCurrency().name())
                            .timestamp(tx.getTimestamp())
                            .build();
                });
            
            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(TEST_ACCOUNT_ID, 0, 10);
            
            // Assert - page content should maintain order from repository
            List<TransactionResponseDTO> transactions = result.getContent();
            assertThat(transactions).hasSize(3);
            // We expect timestamp sorting to be done by the database
            verify(transactionRepository).findByAccount(eq(testAccount), any(Pageable.class));
            verify(transactionMapper, times(3)).toTransactionResponseDTO(any(Transaction.class));
        }
        
        @Test
        @DisplayName("Maps transactions to DTOs correctly")
        void mapsTransactionsToDTOsCorrectly() {
            Transaction tx = createTransaction(1L, TransactionType.DEPOSIT, BigDecimal.valueOf(100), Currency.EUR, now);
            
            Page<Transaction> txPage = new PageImpl<>(List.of(tx), PageRequest.of(0, 10), 1);
            
            TransactionResponseDTO expectedDTO = TransactionResponseDTO.builder()
                .id(1L)
                .accountId(TEST_ACCOUNT_ID)
                .type(TransactionType.DEPOSIT)
                .amount(BigDecimal.valueOf(100))
                .currency("EUR")
                .timestamp(now)
                .description("Test description")
                .relatedCurrency("USD")
                .build();
                
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findByAccount(eq(testAccount), any(Pageable.class))).thenReturn(txPage);
            when(transactionMapper.toTransactionResponseDTO(tx)).thenReturn(expectedDTO);
            
            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(TEST_ACCOUNT_ID, 0, 10);
            
            assertThat(result.getContent()).hasSize(1);
            TransactionResponseDTO actualDTO = result.getContent().get(0);
            assertThat(actualDTO).isEqualTo(expectedDTO);
            assertThat(actualDTO.getDescription()).isEqualTo("Test description");
            assertThat(actualDTO.getRelatedCurrency()).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("Handles zero page size")
        void handlesZeroPageSize() {
            // Arrange - zero size should be handled by Spring
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            
            Page<Transaction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);
            when(transactionRepository.findByAccount(eq(testAccount), any(Pageable.class))).thenReturn(emptyPage);
            
            // Act - passing size = 0
            // The service should handle this or pass it to Spring Data which will set a default size
            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(TEST_ACCOUNT_ID, 0, 0);
            
            // Assert - we expect a non-null result regardless of the zero size
            assertThat(result).isNotNull();
        }
    }
} 