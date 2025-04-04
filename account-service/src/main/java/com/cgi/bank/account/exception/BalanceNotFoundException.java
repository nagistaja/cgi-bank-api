package com.cgi.bank.account.exception;

import com.cgi.bank.account.domain.Currency;

/**
 * Exception thrown when a balance in a specific currency cannot be found for an account.
 */
public class BalanceNotFoundException extends RuntimeException {

    public BalanceNotFoundException(String accountId, Currency currency) {
        super(String.format("Balance not found for account %s in %s currency", accountId, currency));
    }
} 