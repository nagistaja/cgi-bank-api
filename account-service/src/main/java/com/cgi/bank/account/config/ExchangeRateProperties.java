package com.cgi.bank.account.config;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for currency exchange rates.
 * Binds to values under the 'app.exchange-rates' prefix in application configuration.
 * For production, the rates are fetched from the external API.
 */
@ConfigurationProperties(prefix = "app.exchange-rates")
@Getter
@Setter
@Validated // Enable validation on the properties
public class ExchangeRateProperties {

    /**
     * Map of exchange rates keyed by currency pair (e.g., "USD_EUR").
     */
    @NotEmpty // Ensure the map is not empty in the configuration
    private Map<String, BigDecimal> rates;

} 