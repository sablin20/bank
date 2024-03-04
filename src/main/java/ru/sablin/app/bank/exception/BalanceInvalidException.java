package ru.sablin.app.bank.client.exception;

public class BalanceInvalidException extends RuntimeException {
    public BalanceInvalidException(String message) {
        super(message);
    }
}
