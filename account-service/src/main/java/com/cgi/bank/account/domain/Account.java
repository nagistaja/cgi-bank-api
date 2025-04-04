package com.cgi.bank.account.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a bank account which can hold multiple currency balances.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id")
    private String accountId;

    @Version
    private Long version;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @MapKey(name = "currency")
    private Map<Currency, Balance> balances = new HashMap<>();

    /**
     * Gets the balance for the specified currency if it exists.
     *
     * @param currency the currency to get the balance for
     * @return an Optional containing the balance if it exists, otherwise empty
     */
    public Optional<Balance> getBalance(Currency currency) {
        return Optional.ofNullable(this.balances.get(currency));
    }

    /**
     * Gets the existing balance for the specified currency or creates a new one with zero amount.
     *
     * @param currency the currency to get or create a balance for
     * @return the existing or newly created balance
     */
    public Balance getOrCreateBalance(Currency currency) {
        return this.balances.computeIfAbsent(currency, c -> {
            Balance balance = new Balance(this, c, BigDecimal.ZERO);
            return balance;
        });
    }
} 