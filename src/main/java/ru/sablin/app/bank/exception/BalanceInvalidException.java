package ru.sablin.app.bank.exception;

public class BalanceInvalidException extends RuntimeException {
    public BalanceInvalidException(String message) {
        super(message);
    }
}
