package com.cgi.bank.account.service;

import org.springframework.data.domain.Page;

import com.cgi.bank.account.controller.dto.TransactionResponseDTO;

/**
 * Service interface for transaction-related operations.
 */
public interface TransactionService {
    
    /**
     * Retrieves transaction history for a given account with pagination.
     *
     * @param accountId the ID of the account
     * @param page the page number (0-based)
     * @param size the page size
     * @return a page of transaction DTOs
     * @throws com.cgi.bank.account.exception.AccountNotFoundException if the account is not found
     */
    Page<TransactionResponseDTO> getTransactionHistory(String accountId, int page, int size);
} 