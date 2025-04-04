package com.cgi.bank.account.repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.cgi.bank.account.domain.Account;
import com.cgi.bank.account.domain.Balance;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.integration.AbstractIntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

/**
 * Integration tests for the AccountRepository.
 * Tests repository interactions with a real database using Testcontainers.
 */
@Tag("integration")
class AccountRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        // Since some test methods use PROPAGATION_NEVER, we need to handle cleanup in a new transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            accountRepository.deleteAll();
            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    @Test
    @Transactional
    void shouldSaveAndRetrieveAccountWithMultipleBalances() {
        Account account = new Account();
        account.setVersion(0L);

        Map<Currency, Balance> balances = new HashMap<>();
        balances.put(Currency.EUR, new Balance(account, Currency.EUR, new BigDecimal("100.00")));
        balances.put(Currency.USD, new Balance(account, Currency.USD, new BigDecimal("200.00")));
        account.setBalances(balances);

        Account savedAccount = accountRepository.save(account);
        entityManager.flush();
        entityManager.clear();

        String accountId = savedAccount.getAccountId();

        Optional<Account> retrievedAccountOpt = accountRepository.findById(accountId);
        assertThat(retrievedAccountOpt).isPresent();
        
        Account retrievedAccount = retrievedAccountOpt.get();
        assertThat(retrievedAccount.getBalances()).hasSize(2);
        
        Balance eurBalance = retrievedAccount.getBalances().get(Currency.EUR);
        assertThat(eurBalance).isNotNull();
        assertThat(eurBalance.getAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("100.00"));
        
        Balance usdBalance = retrievedAccount.getBalances().get(Currency.USD);
        assertThat(usdBalance).isNotNull();
        assertThat(usdBalance.getAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("200.00"));
    }

    @Test
    void shouldThrowOptimisticLockingExceptionOnConcurrentUpdate() {
        // Step 1: Create an account with an initial balance in a transaction
        Account account = new TransactionTemplate(transactionManager).execute(status -> {
            Account newAccount = new Account();
            newAccount.setVersion(0L);
            
            Balance balance = new Balance(newAccount, Currency.EUR, new BigDecimal("100.00"));
            newAccount.getBalances().put(Currency.EUR, balance);
            
            return accountRepository.saveAndFlush(newAccount);
        });
        
        String accountId = account.getAccountId();
        
        // Step 2: Get two independent entity managers to simulate concurrent transactions
        EntityManager em1 = entityManagerFactory.createEntityManager();
        EntityManager em2 = entityManagerFactory.createEntityManager();
        
        try {
            // Begin first transaction and load the account
            em1.getTransaction().begin();
            Account account1 = em1.find(Account.class, accountId);
            Balance balance1 = account1.getBalances().get(Currency.EUR);
            
            // Begin second transaction and load the same account
            em2.getTransaction().begin();
            final Account account2 = em2.find(Account.class, accountId);
            final Balance balance2 = account2.getBalances().get(Currency.EUR);
            
            // First transaction: modify the balance and commit
            balance1.setAmount(new BigDecimal("150.00"));
            // Also modify the account to ensure version change is tracked
            account1.setVersion(account1.getVersion()); // This triggers dirty checking
            em1.flush();
            em1.getTransaction().commit();
            
            // Second transaction: modify the same balance - this should fail with OptimisticLockException
            balance2.setAmount(new BigDecimal("175.00"));
            // Also modify the account to ensure version change is detected
            account2.setVersion(account2.getVersion()); // This triggers dirty checking
            
            // This should throw OptimisticLockException because the version in the database
            // is now different from what it was when the second transaction began
            assertThatThrownBy(() -> {
                em2.flush();
                em2.getTransaction().commit();
            }).isInstanceOf(OptimisticLockException.class);
            
            // Rollback the second transaction
            if (em2.getTransaction().isActive()) {
                em2.getTransaction().rollback();
            }
        } finally {
            // Clean up
            if (em1.getTransaction().isActive()) {
                em1.getTransaction().rollback();
            }
            if (em2.getTransaction().isActive()) {
                em2.getTransaction().rollback();
            }
            em1.close();
            em2.close();
        }
    }

    @Test
    @Transactional
    void shouldRemoveAccountAndBalancesWhenDeleted() {
        Account account = new Account();
        account.setVersion(0L);
        
        Map<Currency, Balance> balances = new HashMap<>();
        balances.put(Currency.EUR, new Balance(account, Currency.EUR, new BigDecimal("100.00")));
        balances.put(Currency.USD, new Balance(account, Currency.USD, new BigDecimal("200.00")));
        account.setBalances(balances);
        
        Account savedAccount = accountRepository.saveAndFlush(account);
        String accountId = savedAccount.getAccountId();
        entityManager.clear();
        
        accountRepository.deleteById(accountId);
        entityManager.flush();
        entityManager.clear();
        
        assertThat(accountRepository.findById(accountId)).isEmpty();
    }
} 