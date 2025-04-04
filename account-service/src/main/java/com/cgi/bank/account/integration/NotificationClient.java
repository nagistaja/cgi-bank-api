package com.cgi.bank.account.integration;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;

/**
 * Client for sending notifications about account activities.
 * Uses WebClient to make HTTP calls to an external notification service.
 * Implements circuit breaker, retry, and timeout patterns using Resilience4j.
 */
@Slf4j
@Component
public class NotificationClient {

    private final WebClient webClient;
    
    /**
     * Constructs the notification client with the configured external service URL.
     * 
     * @param webClientBuilder The preconfigured WebClient.Builder
     * @param notificationUrl The base URL for the notification service
     */
    public NotificationClient(WebClient.Builder webClientBuilder, 
                             @Value("${app.notification.url}") String notificationUrl) {
        this.webClient = webClientBuilder
                .baseUrl(notificationUrl)
                .build();
        log.info("NotificationClient initialized with base URL: {}", notificationUrl);
    }
    
    /**
     * Sends an asynchronous notification for a deposit event.
     * Applies circuit breaker, retry, and timeout patterns using Resilience4j.
     * 
     * @param accountId The ID of the account where the deposit occurred
     * @param amount The amount that was deposited
     * @param currency The currency of the deposit
     * @return A CompletableFuture that will be completed when the notification is sent
     */
    @Async("taskExecutor")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendDepositNotificationFallback")
    @Retry(name = "notificationService")
    @TimeLimiter(name = "notificationService")
    public CompletableFuture<Void> sendDepositNotification(String accountId, String amount, String currency) {
        log.info("Sending deposit notification for account: {}, amount: {} {}", accountId, amount, currency);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/200")
                        .queryParam("accountId", accountId)
                        .queryParam("amount", amount)
                        .queryParam("currency", currency)
                        .queryParam("type", "DEPOSIT")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Deposit notification sent successfully for account: {}", accountId))
                .doOnError(error -> log.warn("Error sending notification for account: {}, error: {}", 
                        accountId, error.getMessage()))
                .then()
                .toFuture();
    }
    
    /**
     * Fallback method for the circuit breaker.
     * Used when the notification service call fails due to circuit breaker being open.
     * 
     * @param accountId The ID of the account
     * @param amount The transaction amount
     * @param currency The transaction currency
     * @param exception The exception that triggered the fallback
     * @return A CompletableFuture that will be completed immediately with a null value
     */
    public CompletableFuture<Void> sendDepositNotificationFallback(String accountId, String amount, 
                                                                  String currency, Throwable exception) {
        logNotificationFailure(accountId, amount, currency, exception);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Logs a notification failure.
     * Used as a fallback when the notification service call fails.
     * 
     * @param accountId The ID of the account
     * @param amount The transaction amount
     * @param currency The transaction currency
     * @param error The exception that occurred
     */
    private void logNotificationFailure(String accountId, String amount, String currency, Throwable error) {
        log.warn("Circuit breaker triggered for notification to account: {}, amount: {} {}, error: {}", 
                accountId, amount, currency, error.getMessage());
    }
} 