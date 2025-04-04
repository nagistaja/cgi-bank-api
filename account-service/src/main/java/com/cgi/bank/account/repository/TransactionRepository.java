package com.cgi.bank.account.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cgi.bank.account.domain.Account;
import com.cgi.bank.account.domain.Transaction;

/**
 * Repository for managing Transaction entities.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Find all transactions for a specific account.
     * 
     * @param account the account to find transactions for
     * @param pageable pagination information
     * @return a page of transactions
     */
    Page<Transaction> findByAccount(Account account, Pageable pageable);
} 