package com.cgi.bank.account.service.mapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cgi.bank.account.controller.dto.BalanceDTO;
import com.cgi.bank.account.domain.Account;
import com.cgi.bank.account.domain.Balance;
import com.cgi.bank.account.domain.Currency;

@ExtendWith(MockitoExtension.class)
class BalanceMapperTest {

    @Mock
    private Account account;

    @InjectMocks
    private BalanceMapperImpl balanceMapper;

    @Test
    void toBalanceDTO_shouldMapBalanceToDTO() {
        Balance balance = new Balance(account, Currency.EUR, BigDecimal.valueOf(100));
        
        BalanceDTO dto = balanceMapper.toBalanceDTO(balance);
        
        assertThat(dto).isNotNull();
        assertThat(dto.getCurrency()).isEqualTo(Currency.EUR.name());
        assertThat(dto.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
    
    @Test
    void toBalanceDTO_shouldReturnNull_whenBalanceIsNull() {
        BalanceDTO dto = balanceMapper.toBalanceDTO(null);
        
        assertThat(dto).isNull();
    }
    
    @Test
    void currencyToString_shouldConvertCurrencyToString() {
        Currency currency = Currency.USD;
        
        String result = balanceMapper.currencyToString(currency);
        
        assertThat(result).isEqualTo("USD");
    }
    
    @Test
    void currencyToString_shouldReturnNull_whenCurrencyIsNull() {
        String result = balanceMapper.currencyToString(null);
        
        assertThat(result).isNull();
    }
    
    @Test
    void mapMultipleBalancesToDTOs() {
        Balance balance1 = new Balance(account, Currency.EUR, BigDecimal.valueOf(100));
        Balance balance2 = new Balance(account, Currency.USD, BigDecimal.valueOf(200));
        List<Balance> balances = Arrays.asList(balance1, balance2);
        
        List<BalanceDTO> dtos = balances.stream()
                .map(balanceMapper::toBalanceDTO)
                .collect(Collectors.toList());
        
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getCurrency()).isEqualTo(Currency.EUR.name());
        assertThat(dtos.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(dtos.get(1).getCurrency()).isEqualTo(Currency.USD.name());
        assertThat(dtos.get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }
} 