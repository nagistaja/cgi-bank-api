package com.cgi.bank.account.service;

import java.math.BigDecimal;

import com.cgi.bank.account.controller.dto.AccountBalanceResponseDTO;
import com.cgi.bank.account.domain.Currency;

/**
 * Service interface for account-related operations.
 */
public interface AccountService {
    
    /**
     * Creates a new empty account.
     *
     * @return a DTO containing the new account ID and empty balances list
     */
    AccountBalanceResponseDTO createAccount();
    
    /**
     * Retrieves all balances for a given account.
     *
     * @param accountId the ID of the account
     * @return a DTO containing the account ID and all balances
     * @throws com.cgi.bank.account.exception.AccountNotFoundException if the account is not found
     */
    AccountBalanceResponseDTO getAccountBalances(String accountId);
    
    /**
     * Deposits money into an account in the specified currency.
     *
     * @param accountId the ID of the account
     * @param amount the amount to deposit
     * @param currency the currency of the deposit
     * @return a DTO containing the account ID and updated balances
     * @throws com.cgi.bank.account.exception.AccountNotFoundException if the account is not found
     * @throws IllegalArgumentException if the amount is not positive
     */
    AccountBalanceResponseDTO deposit(String accountId, BigDecimal amount, Currency currency);
    
    /**
     * Withdraws money from an account in the specified currency.
     *
     * @param accountId the ID of the account
     * @param amount the amount to withdraw
     * @param currency the currency of the withdrawal
     * @return a DTO containing the account ID and updated balances
     * @throws com.cgi.bank.account.exception.AccountNotFoundException if the account is not found
     * @throws com.cgi.bank.account.exception.BalanceNotFoundException if the account has no balance 
     *         in the specified currency
     * @throws com.cgi.bank.account.exception.InsufficientFundsException if the account has insufficient funds
     * @throws IllegalArgumentException if the amount is not positive
     */
    AccountBalanceResponseDTO withdraw(String accountId, BigDecimal amount, Currency currency);
    
    /**
     * Exchanges money from one currency to another within an account.
     *
     * @param accountId the ID of the account
     * @param fromCurrency the source currency
     * @param toCurrency the target currency
     * @param amount the amount to exchange in the source currency
     * @return a DTO containing the account ID and updated balances
     * @throws com.cgi.bank.account.exception.AccountNotFoundException if the account is not found
     * @throws com.cgi.bank.account.exception.BalanceNotFoundException if the account has no balance 
     *         in the source currency
     * @throws com.cgi.bank.account.exception.InsufficientFundsException if the account has insufficient funds
     *         in the source currency
     * @throws com.cgi.bank.account.exception.InvalidCurrencyException if the exchange rate is not found
     *         for the currency pair
     * @throws IllegalArgumentException if the amount is not positive or if source and target currencies are the same
     */
    AccountBalanceResponseDTO exchange(String accountId, Currency fromCurrency, Currency toCurrency, 
            BigDecimal amount);
} 