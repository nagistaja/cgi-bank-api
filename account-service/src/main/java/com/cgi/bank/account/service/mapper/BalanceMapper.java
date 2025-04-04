package com.cgi.bank.account.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.cgi.bank.account.controller.dto.BalanceDTO;
import com.cgi.bank.account.domain.Balance;
import com.cgi.bank.account.domain.Currency;

/**
 * MapStruct mapper for mapping between {@link Balance} and {@link BalanceDTO}.
 */
@Mapper(componentModel = "spring")
public interface BalanceMapper {

    /**
     * Converts a Balance entity to a BalanceDTO.
     *
     * @param balance the balance entity to convert
     * @return the corresponding BalanceDTO
     */
    @Mapping(source = "currency", target = "currency", qualifiedByName = "currencyToString")
    BalanceDTO toBalanceDTO(Balance balance);

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
} 