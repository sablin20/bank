package ru.sablin.app.bank.exception;

public class EmailBusyException extends RuntimeException {
    public EmailBusyException(String message) {
        super(message);
    }
}
