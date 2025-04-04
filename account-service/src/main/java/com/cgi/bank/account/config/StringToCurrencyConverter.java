package com.cgi.bank.account.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.exception.InvalidCurrencyException;

/**
 * Converts String currency codes to Currency enum values.
 * Throws InvalidCurrencyException for unsupported or invalid currencies.
 */
@Component
public class StringToCurrencyConverter implements Converter<String, Currency> {

    @Override
    public Currency convert(String  source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        
        String upperSource = source.trim().toUpperCase();
        try {
            return Currency.valueOf(upperSource);
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException(source);
        }
    }
} 