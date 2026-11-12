package com.snovasys.ledgerline.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("The idempotency key has already been used for a different request.");
    }
}
