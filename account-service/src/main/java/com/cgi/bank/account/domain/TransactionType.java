package com.cgi.bank.account.domain;

/**
 * Represents the types of transactions that can occur in the account-service.
 */
public enum TransactionType {
    DEPOSIT,       // Money added to an account
    WITHDRAWAL,    // Money removed from an account
    EXCHANGE_FROM, // Money removed as part of a currency exchange
    EXCHANGE_TO    // Money added as part of a currency exchange
} 