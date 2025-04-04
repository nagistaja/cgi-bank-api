package com.cgi.bank.account.exception;

/**
 * Exception thrown when an invalid or unsupported currency is used.
 */
public class InvalidCurrencyException extends RuntimeException {

    public InvalidCurrencyException(String currency) {
        super("Invalid or unsupported currency: " + currency);
    }
    
    public InvalidCurrencyException(String fromCurrency, String toCurrency) {
        super("Exchange not supported between currencies: " + fromCurrency + " and " + toCurrency);
    }
} 