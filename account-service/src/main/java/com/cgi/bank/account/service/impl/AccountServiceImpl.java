package com.cgi.bank.account.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.bank.account.controller.dto.AccountBalanceResponseDTO;
import com.cgi.bank.account.controller.dto.BalanceDTO;
import com.cgi.bank.account.domain.Account;
import com.cgi.bank.account.domain.Balance;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.domain.Transaction;
import com.cgi.bank.account.exception.AccountNotFoundException;
import com.cgi.bank.account.exception.BalanceNotFoundException;
import com.cgi.bank.account.exception.OptimisticLockingConflictException;
import com.cgi.bank.account.integration.NotificationClient;
import com.cgi.bank.account.repository.AccountRepository;
import com.cgi.bank.account.repository.TransactionRepository;
import com.cgi.bank.account.service.AccountService;
import com.cgi.bank.account.service.CurrencyExchangeService;
import com.cgi.bank.account.service.mapper.BalanceMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the AccountService interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CurrencyExchangeService currencyExchangeService;
    private final NotificationClient notificationClient;
    private final BalanceMapper balanceMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AccountBalanceResponseDTO createAccount() {
        log.debug("Creating a new account");
        
        Account account = new Account();
        account = accountRepository.save(account);
        
        return new AccountBalanceResponseDTO(account.getAccountId(), Collections.emptyList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AccountBalanceResponseDTO getAccountBalances(String accountId) {
        log.debug("Retrieving balances for account: {}", accountId);
        
        Account account = accountRepository.findByIdWithBalances(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        
        List<BalanceDTO> balanceDTOs = account.getBalances().values().stream()
                .map(balanceMapper::toBalanceDTO)
                .collect(Collectors.toList());
        
        return new AccountBalanceResponseDTO(accountId, balanceDTOs);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AccountBalanceResponseDTO deposit(String accountId, BigDecimal amount, Currency currency) {
        log.debug("Depositing {} {} into account: {}", amount, currency, accountId);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        
        Account account = accountRepository.findByIdWithBalances(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        
        Balance balance = account.getOrCreateBalance(currency);
        balance.addAmount(amount);
        
        Transaction transaction = Transaction.createDeposit(account, currency, amount);
        transactionRepository.save(transaction);
        
        account = accountRepository.save(account);
        
        notificationClient.sendDepositNotification(
                accountId, 
                amount.toString(), 
                currency.name());
        
        List<BalanceDTO> balanceDTOs = account.getBalances().values().stream()
                .map(balanceMapper::toBalanceDTO)
                .collect(Collectors.toList());
        
        return new AccountBalanceResponseDTO(accountId, balanceDTOs);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AccountBalanceResponseDTO withdraw(String accountId, BigDecimal amount, Currency currency) {
        log.debug("Withdrawing {} {} from account: {}", amount, currency, accountId);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        
        Account account = accountRepository.findByIdWithBalances(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        
        Balance balance = account.getBalance(currency)
                .orElseThrow(() -> new BalanceNotFoundException(accountId, currency));
        
        try {
            // This will throw InsufficientFundsException if balance is insufficient
            balance.subtractAmount(amount);
            
            Transaction transaction = Transaction.createWithdrawal(account, currency, amount);
            transactionRepository.save(transaction);
            
            account = accountRepository.save(account);
            
            List<BalanceDTO> balanceDTOs = account.getBalances().values().stream()
                    .map(balanceMapper::toBalanceDTO)
                    .collect(Collectors.toList());
            
            return new AccountBalanceResponseDTO(accountId, balanceDTOs);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Optimistic locking conflict during withdrawal for account: {}", accountId, e);
            throw new OptimisticLockingConflictException(
                    "Another operation modified the balance while your withdrawal was processing. " +
                    "Please try again.", e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AccountBalanceResponseDTO exchange(String accountId, Currency fromCurrency, Currency toCurrency, 
            BigDecimal amount) {
        log.debug("Exchanging {} {} to {} for account: {}", amount, fromCurrency, toCurrency, accountId);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange amount must be positive");
        }
        
        if (fromCurrency == toCurrency) {
            throw new IllegalArgumentException("Source and target currencies must be different");
        }
        
        Account account = accountRepository.findByIdWithBalances(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        
        Balance fromBalance = account.getBalance(fromCurrency)
                .orElseThrow(() -> new BalanceNotFoundException(accountId, fromCurrency));
        
        try {
            BigDecimal exchangedAmount = currencyExchangeService.calculateExchange(
                    fromCurrency, toCurrency, amount);
            log.debug("Exchanged amount: {} {} = {} {}", amount, fromCurrency, exchangedAmount, toCurrency);
            
            // Subtract from source balance (will throw InsufficientFundsException if insufficient)
            fromBalance.subtractAmount(amount);
            
            Balance toBalance = account.getOrCreateBalance(toCurrency);
            toBalance.addAmount(exchangedAmount);
            
            Transaction fromTransaction = Transaction.createExchangeFrom(account, fromCurrency, amount);
            Transaction toTransaction = Transaction.createExchangeTo(account, toCurrency, exchangedAmount);
            transactionRepository.save(fromTransaction);
            transactionRepository.save(toTransaction);
            
            account = accountRepository.save(account);
            
            List<BalanceDTO> balanceDTOs = account.getBalances().values().stream()
                    .map(balanceMapper::toBalanceDTO)
                    .collect(Collectors.toList());
            
            return new AccountBalanceResponseDTO(accountId, balanceDTOs);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Optimistic locking conflict during exchange for account: {}", accountId, e);
            throw new OptimisticLockingConflictException(
                    "Another operation modified the balance while your exchange was processing. " + 
                    "Please try again.", e);
        }
    }
} 