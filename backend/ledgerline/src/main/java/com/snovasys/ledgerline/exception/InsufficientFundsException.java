package com.snovasys.ledgerline.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("The account has insufficient funds.");
    }
}
