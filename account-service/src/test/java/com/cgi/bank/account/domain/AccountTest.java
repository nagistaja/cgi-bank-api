package com.cgi.bank.account.domain;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
    }

    @Test
    void getBalance_shouldReturnEmptyOptional_whenBalanceForCurrencyDoesNotExist() {
        Optional<Balance> result = account.getBalance(Currency.EUR);

        assertThat(result).isEmpty();
    }

    @Test
    void getBalance_shouldReturnCorrectBalance_whenBalanceForCurrencyExists() {
        Balance eurBalance = new Balance(account, Currency.EUR, BigDecimal.TEN);
        account.getBalances().put(Currency.EUR, eurBalance);

        Optional<Balance> result = account.getBalance(Currency.EUR);

        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(eurBalance);
    }

    @Test
    void getOrCreateBalance_shouldCreateNewBalance_whenBalanceForCurrencyDoesNotExist() {
        Currency currency = Currency.USD;

        Balance result = account.getOrCreateBalance(currency);

        assertThat(result).isNotNull();
        assertThat(result.getCurrency()).isEqualTo(currency);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getAccount()).isSameAs(account);
        assertThat(account.getBalances()).containsKey(currency);
        assertThat(account.getBalances().get(currency)).isSameAs(result);
    }

    @Test
    void getOrCreateBalance_shouldReturnExistingBalance_whenBalanceForCurrencyExists() {
        Currency currency = Currency.EUR;
        Balance existingBalance = new Balance(account, currency, BigDecimal.valueOf(100));
        account.getBalances().put(currency, existingBalance);

        Balance result = account.getOrCreateBalance(currency);

        assertThat(result).isSameAs(existingBalance);
    }
} 