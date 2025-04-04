package com.cgi.bank.account.exception;

/**
 * Exception thrown when an account cannot be found.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("Account not found with ID: " + accountId);
    }

    public AccountNotFoundException(String accountId, Throwable cause) {
        super("Account not found with ID: " + accountId, cause);
    }
} 