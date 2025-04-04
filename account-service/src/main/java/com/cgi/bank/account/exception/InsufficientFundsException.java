package com.cgi.bank.account.exception;

import java.math.BigDecimal;

import com.cgi.bank.account.domain.Currency;

/**
 * Exception thrown when there are insufficient funds for a withdrawal or exchange operation.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String accountId, Currency currency, BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient funds for account %s in %s currency. Requested: %s, Available: %s", 
                accountId, currency, requested, available));
    }
} 