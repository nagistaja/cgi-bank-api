package com.cgi.bank.account.exception;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Exception thrown when a concurrent update on the same entity is detected (optimistic locking conflict).
 */
public class OptimisticLockingConflictException extends RuntimeException {

    public OptimisticLockingConflictException(String message) {
        super(message);
    }

    public OptimisticLockingConflictException(String message, ObjectOptimisticLockingFailureException cause) {
        super(message, cause);
    }
} 