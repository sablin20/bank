package ru.sablin.app.bank.client.exception;

public class EmailBusyException extends RuntimeException {
    public EmailBusyException(String message) {
        super(message);
    }
}
