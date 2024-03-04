package ru.sablin.app.bank.client.exception;

public class MoneyInvalidException extends RuntimeException {
    public MoneyInvalidException(String message) {
        super(message);
    }
}
