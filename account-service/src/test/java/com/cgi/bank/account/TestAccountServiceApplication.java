package com.cgi.bank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test application for manual testing with Testcontainers.
 * This application can be run during development to use Testcontainers
 * instead of requiring a local PostgreSQL instance.
 */
public class TestAccountServiceApplication {

    /**
     * Main method to run the application with Testcontainers.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.from(AccountServiceApplication::main)
            .with(TestContainerConfiguration.class)
            .run(args);
    }
    
    /**
     * TestConfiguration for configuring Testcontainers with the service connection.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestContainerConfiguration {

        /**
         * Creates and configures a PostgreSQL container for testing.
         * 
         * @return a configured PostgreSQL container
         */
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("accounts_db")
                .withUsername("postgres")
                .withPassword("password");
        }
    }
}
