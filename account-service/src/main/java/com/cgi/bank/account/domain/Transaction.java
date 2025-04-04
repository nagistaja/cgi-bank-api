package com.cgi.bank.account.domain;

import java.math.BigDecimal;
import java.time.Instant;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a transaction record for audit purposes.
 * Tracks deposits, withdrawals, and currency exchanges.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "account")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Creates a new transaction.
     *
     * @param account  the account the transaction belongs to
     * @param type     the type of transaction
     * @param currency the currency of the transaction
     * @param amount   the amount of the transaction
     */
    public Transaction(Account account, TransactionType type, Currency currency, BigDecimal amount) {
        this.account = account;
        this.type = type;
        this.currency = currency;
        this.amount = amount;
        this.timestamp = Instant.now();
    }

    /**
     * Factory method to create a deposit transaction.
     *
     * @param account  the account the deposit is for
     * @param currency the currency of the deposit
     * @param amount   the amount being deposited
     * @return a new deposit transaction
     */
    public static Transaction createDeposit(Account account, Currency currency, BigDecimal amount) {
        return new Transaction(account, TransactionType.DEPOSIT, currency, amount);
    }

    /**
     * Factory method to create a withdrawal transaction.
     *
     * @param account  the account the withdrawal is from
     * @param currency the currency of the withdrawal
     * @param amount   the amount being withdrawn
     * @return a new withdrawal transaction
     */
    public static Transaction createWithdrawal(Account account, Currency currency, BigDecimal amount) {
        return new Transaction(account, TransactionType.WITHDRAWAL, currency, amount);
    }

    /**
     * Factory method to create a currency exchange "from" transaction.
     *
     * @param account      the account the exchange is for
     * @param fromCurrency the currency being exchanged from
     * @param amount       the amount being exchanged
     * @return a new exchange from transaction
     */
    public static Transaction createExchangeFrom(Account account, Currency fromCurrency, BigDecimal amount) {
        return new Transaction(account, TransactionType.EXCHANGE_FROM, fromCurrency, amount);
    }

    /**
     * Factory method to create a currency exchange "to" transaction.
     *
     * @param account    the account the exchange is for
     * @param toCurrency the currency being exchanged to
     * @param amount     the amount after exchange
     * @return a new exchange to transaction
     */
    public static Transaction createExchangeTo(Account account, Currency toCurrency, BigDecimal amount) {
        return new Transaction(account, TransactionType.EXCHANGE_TO, toCurrency, amount);
    }
} 