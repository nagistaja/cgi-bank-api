package com.cgi.bank.account.domain;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cgi.bank.account.exception.InsufficientFundsException;

@ExtendWith(MockitoExtension.class)
class BalanceTest {

    @Mock
    private Account account;

    private Balance balance;
    private Balance balanceForInsufficientFunds;

    @BeforeEach
    void setUp() {
        // Use lenient stubbing since not all tests will use this mock
        lenient().when(account.getAccountId()).thenReturn("test-account-id");
        
        // Regular balance for most tests
        balance = new Balance(null, Currency.EUR, BigDecimal.valueOf(100.0));
        
        // Only use account mock for insufficient funds test
        balanceForInsufficientFunds = new Balance(account, Currency.EUR, BigDecimal.valueOf(100.0));
    }

    @Test
    void addAmount_shouldIncreaseBalanceWhenPositiveAmount() {
        BigDecimal initialAmount = balance.getAmount();
        BigDecimal amountToAdd = BigDecimal.valueOf(50.0);

        balance.addAmount(amountToAdd);

        assertThat(balance.getAmount())
                .isEqualByComparingTo(initialAmount.add(amountToAdd));
    }

    @Test
    void addAmount_shouldThrowException_whenAmountIsZero() {
        BigDecimal amountToAdd = BigDecimal.ZERO;

        assertThatThrownBy(() -> balance.addAmount(amountToAdd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount to add must be positive");
    }

    @Test
    void addAmount_shouldThrowException_whenAmountIsNegative() {
        BigDecimal amountToAdd = BigDecimal.valueOf(-50.0);

        assertThatThrownBy(() -> balance.addAmount(amountToAdd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount to add must be positive");
    }

    @Test
    void subtractAmount_shouldDecreaseBalance_whenAmountIsLessThanAvailable() {
        BigDecimal initialAmount = balance.getAmount();
        BigDecimal amountToSubtract = BigDecimal.valueOf(50.0);

        balance.subtractAmount(amountToSubtract);

        assertThat(balance.getAmount())
                .isEqualByComparingTo(initialAmount.subtract(amountToSubtract));
    }

    @Test
    void subtractAmount_shouldDecreaseBalanceToZero_whenAmountEqualsAvailable() {
        BigDecimal amountToSubtract = balance.getAmount();

        balance.subtractAmount(amountToSubtract);

        assertThat(balance.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void subtractAmount_shouldThrowException_whenAmountIsZero() {
        BigDecimal amountToSubtract = BigDecimal.ZERO;

        assertThatThrownBy(() -> balance.subtractAmount(amountToSubtract))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount to subtract must be positive");
    }

    @Test
    void subtractAmount_shouldThrowException_whenAmountIsNegative() {
        BigDecimal amountToSubtract = BigDecimal.valueOf(-50.0);

        assertThatThrownBy(() -> balance.subtractAmount(amountToSubtract))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount to subtract must be positive");
    }

    @Test
    void subtractAmount_shouldThrowException_whenAmountExceedsAvailable() {
        BigDecimal amountToSubtract = balanceForInsufficientFunds.getAmount().add(BigDecimal.ONE);

        assertThatThrownBy(() -> balanceForInsufficientFunds.subtractAmount(amountToSubtract))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds")
                .hasMessageContaining(balanceForInsufficientFunds.getCurrency().name());
    }
} 