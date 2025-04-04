package com.cgi.bank.account.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cgi.bank.account.config.ExchangeRateProperties;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.exception.InvalidCurrencyException;

@ExtendWith(MockitoExtension.class)
class CurrencyExchangeServiceImplTest {

    @Mock
    private ExchangeRateProperties exchangeRateProperties;

    @InjectMocks
    private CurrencyExchangeServiceImpl currencyExchangeService;

    private Map<String, BigDecimal> ratesMap;

    @BeforeEach
    void setUp() {
        ratesMap = new HashMap<>();
        ratesMap.put("EUR_USD", BigDecimal.valueOf(1.08));
        ratesMap.put("USD_EUR", BigDecimal.valueOf(0.92));
        ratesMap.put("EUR_SEK", BigDecimal.valueOf(10.5));
        ratesMap.put("USD_SEK", BigDecimal.valueOf(9.7));
        
        lenient().when(exchangeRateProperties.getRates()).thenReturn(ratesMap);
    }

    @Test
    void calculateExchange_shouldReturnCorrectAmount_whenValidExchange() {
        Currency from = Currency.EUR;
        Currency to = Currency.USD;
        BigDecimal amount = BigDecimal.valueOf(100.0);
        BigDecimal expectedAmount = amount.multiply(ratesMap.get("EUR_USD"))
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal result = currencyExchangeService.calculateExchange(from, to, amount);

        assertThat(result).isEqualByComparingTo(expectedAmount);
    }

    @Test
    void calculateExchange_shouldReturnOriginalAmount_whenSameCurrency() {
        Currency currency = Currency.EUR;
        BigDecimal amount = BigDecimal.valueOf(100.0);

        BigDecimal result = currencyExchangeService.calculateExchange(currency, currency, amount);

        assertThat(result).isEqualByComparingTo(amount);
    }

    @Test
    void calculateExchange_shouldThrowException_whenNegativeAmount() {
        Currency from = Currency.EUR;
        Currency to = Currency.USD;
        BigDecimal amount = BigDecimal.valueOf(-100.0);

        assertThatThrownBy(() -> currencyExchangeService.calculateExchange(from, to, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exchange amount must be positive");
    }

    @Test
    void calculateExchange_shouldThrowException_whenZeroAmount() {
        Currency from = Currency.EUR;
        Currency to = Currency.USD;
        BigDecimal amount = BigDecimal.ZERO;

        assertThatThrownBy(() -> currencyExchangeService.calculateExchange(from, to, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exchange amount must be positive");
    }

    @Test
    void calculateExchange_shouldThrowException_whenMissingRate() {
        Currency from = Currency.EUR;
        Currency to = Currency.RUB;
        BigDecimal amount = BigDecimal.valueOf(100.0);
        
        assertThatThrownBy(() -> currencyExchangeService.calculateExchange(from, to, amount))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
    }

    @Test
    void calculateExchange_shouldThrowException_whenRatesMapIsNull() {
        Currency from = Currency.EUR;
        Currency to = Currency.USD;
        BigDecimal amount = BigDecimal.valueOf(100.0);
        
        when(exchangeRateProperties.getRates()).thenReturn(null);

        assertThatThrownBy(() -> currencyExchangeService.calculateExchange(from, to, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exchange rates configuration is missing or invalid");
    }
} 