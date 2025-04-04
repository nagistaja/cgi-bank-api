package com.cgi.bank.account.domain;

import java.math.BigDecimal;

import com.cgi.bank.account.exception.InsufficientFundsException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a currency balance for an account.
 * Each account can have multiple balances, one per currency.
 */
@Entity
@Table(name = "balances", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "currency"}))
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@ToString(exclude = "account")
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @Version
    private Long version;

    /**
     * Creates a new balance for an account in the specified currency.
     *
     * @param account  the account this balance belongs to
     * @param currency the currency of this balance
     * @param amount   the initial amount (can be zero)
     */
    public Balance(Account account, Currency currency, BigDecimal amount) {
        this.account = account;
        this.currency = currency;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    /**
     * Adds the specified amount to the balance.
     *
     * @param value the amount to add (must be positive)
     * @throws IllegalArgumentException if value is negative or zero
     */
    public void addAmount(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount to add must be positive");
        }
        this.amount = this.amount.add(value);
    }

    /**
     * Subtracts the specified amount from the balance if sufficient funds are available.
     *
     * @param value the amount to subtract (must be positive)
     * @throws IllegalArgumentException if value is negative or zero
     * @throws InsufficientFundsException if the balance is less than the amount to subtract
     */
    public void subtractAmount(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount to subtract must be positive");
        }
        
        if (this.amount.compareTo(value) < 0) {
            throw new InsufficientFundsException(
                    account.getAccountId(), 
                    currency, 
                    value, 
                    amount
            );
        }
        
        this.amount = this.amount.subtract(value);
    }
} 