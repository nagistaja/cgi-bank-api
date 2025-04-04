package com.cgi.bank.account.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cgi.bank.account.config.ExchangeRateProperties;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.exception.InvalidCurrencyException;
import com.cgi.bank.account.service.CurrencyExchangeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the CurrencyExchangeService interface.
 * Handles currency exchange operations based on configured exchange rates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyExchangeServiceImpl implements CurrencyExchangeService {

    private static final int DECIMAL_PLACES = 4;

    private final ExchangeRateProperties exchangeRateProperties;

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
    @Override
    public BigDecimal calculateExchange(Currency from, Currency to, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange amount must be positive");
        }
        
        if (from == to) {
            return amount;
        }
        
        String key = from.name() + "_" + to.name();
        Map<String, BigDecimal> rates = exchangeRateProperties.getRates();
        
        if (rates == null) {
            log.error(
                "Exchange rates map is null within the injected ExchangeRateProperties bean. Configuration issue?");
            throw new IllegalStateException("Exchange rates configuration is missing or invalid");
        }
        
        BigDecimal rate = rates.get(key);
        
        if (rate == null) {
            log.error("Exchange rate not found for pair: {}. Available rates: {}", key, rates.keySet());
            throw new InvalidCurrencyException(from.name(), to.name());
        }
        
        return amount.multiply(rate).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);
    }
} 