package com.cgi.bank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.cgi.bank.account.config.ExchangeRateProperties;

/**
 * Main application class for the account-service.
 */
@SpringBootApplication
@EnableConfigurationProperties(ExchangeRateProperties.class)
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

}
