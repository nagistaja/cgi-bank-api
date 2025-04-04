package com.cgi.bank.account.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that require a PostgreSQL database.
 * Uses static Testcontainers setup for improved performance across test executions.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(parallel = true)
public abstract class AbstractIntegrationTest {
    
    /**
     * Static PostgreSQL container shared across all test classes that extend this class.
     * Using a static container improves test performance by reusing the same container.
     */
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER = 
            new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withCommand(
                    "postgres",
                    "-c", "fsync=off",
                    "-c", "max_connections=50", 
                    "-c", "shared_buffers=256MB",
                    "-c", "synchronous_commit=off",
                    "-c", "full_page_writes=off"
                )
                .withReuse(true);
    
    static {
        POSTGRES_CONTAINER.start();
    }
    
    /**
     * Dynamically sets Spring datasource properties based on the running PostgreSQL container.
     * 
     * @param registry the dynamic property registry to configure
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        
        // Hibernate settings
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.connection.isolation", () -> 2); // READ_COMMITTED
        
        // HikariCP settings
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 5);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 1);
        registry.add("spring.datasource.hikari.transaction-isolation", () -> "TRANSACTION_READ_COMMITTED");
    }
} 