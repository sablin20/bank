package ru.sablin.app.bank.exception;

public class MoneyInvalidException extends RuntimeException {
    public MoneyInvalidException(String message) {
        super(message);
    }
}
