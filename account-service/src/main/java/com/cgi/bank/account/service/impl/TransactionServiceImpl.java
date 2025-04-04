package com.cgi.bank.account.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.bank.account.controller.dto.TransactionResponseDTO;
import com.cgi.bank.account.domain.Account;
import com.cgi.bank.account.domain.Transaction;
import com.cgi.bank.account.exception.AccountNotFoundException;
import com.cgi.bank.account.repository.AccountRepository;
import com.cgi.bank.account.repository.TransactionRepository;
import com.cgi.bank.account.service.TransactionService;
import com.cgi.bank.account.service.mapper.TransactionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link TransactionService} interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> getTransactionHistory(String accountId, int page, int size) {
        log.debug("Finding transaction history for account ID: {}, page: {}, size: {}", accountId, page, size);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        Page<Transaction> transactions = transactionRepository.findByAccount(account, pageRequest);
        
        return transactions.map(transactionMapper::toTransactionResponseDTO);
    }
} 