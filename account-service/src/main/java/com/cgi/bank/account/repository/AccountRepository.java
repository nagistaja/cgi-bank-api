package com.cgi.bank.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cgi.bank.account.domain.Account;

/**
 * Repository for managing Account entities.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    
    /**
     * Finds an account by ID and eagerly fetches its balances.
     *
     * @param accountId the account ID to search for
     * @return an Optional containing the account with balances if found
     */
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.balances WHERE a.accountId = :accountId")
    Optional<Account> findByIdWithBalances(@Param("accountId") String accountId);
} 