package com.cgi.bank.account.service;

import java.math.BigDecimal;

import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.exception.InvalidCurrencyException;

/**
 * Interface for handling currency exchange operations.
 */
public interface CurrencyExchangeService {

    /**
     * Calculates the exchange amount from one currency to another.
     *
     * @param from the source currency
     * @param to the target currency
     * @param amount the amount to exchange
     * @return the calculated amount in the target currency
     * @throws IllegalArgumentException if the amount is not positive
     * @throws InvalidCurrencyException if the exchange rate for the currency pair is not found
     */
    BigDecimal calculateExchange(Currency from, Currency to, BigDecimal amount);
} 