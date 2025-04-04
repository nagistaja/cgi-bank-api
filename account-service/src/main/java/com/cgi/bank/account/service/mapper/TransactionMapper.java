package com.cgi.bank.account.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.cgi.bank.account.controller.dto.TransactionResponseDTO;
import com.cgi.bank.account.domain.Currency;
import com.cgi.bank.account.domain.Transaction;

/**
 * MapStruct mapper for mapping between {@link Transaction} and {@link TransactionResponseDTO}.
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    /**
     * Converts a Transaction entity to a TransactionResponseDTO.
     *
     * @param transaction the transaction entity to convert
     * @return the corresponding TransactionResponseDTO
     */
    @Mapping(source = "account.accountId", target = "accountId")
    @Mapping(source = "currency", target = "currency", qualifiedByName = "currencyToString")
    @Mapping(target = "relatedCurrency", constant = "")
    @Mapping(target = "description", expression = "java(generateDescription(transaction))")
    TransactionResponseDTO toTransactionResponseDTO(Transaction transaction);

    /**
     * Converts a Currency enum to a String.
     *
     * @param currency the Currency enum value
     * @return the string representation of the currency
     */
    @Named("currencyToString")
    default String currencyToString(Currency currency) {
        return currency == null ? null : currency.name();
    }
    
    /**
     * Generates a description for the transaction based on its type and currency.
     *
     * @param transaction the transaction
     * @return a description string
     */
    default String generateDescription(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        
        switch (transaction.getType()) {
            case DEPOSIT:
                return "Deposit of " + transaction.getAmount() + " " + transaction.getCurrency();
            case WITHDRAWAL:
                return "Withdrawal of " + transaction.getAmount() + " " + transaction.getCurrency();
            case EXCHANGE_FROM:
                return "Exchange from " + transaction.getAmount() + " " + transaction.getCurrency();
            case EXCHANGE_TO:
                return "Exchange to " + transaction.getAmount() + " " + transaction.getCurrency();
            default:
                return "Transaction of " + transaction.getAmount() + " " + transaction.getCurrency();
        }
    }
} 