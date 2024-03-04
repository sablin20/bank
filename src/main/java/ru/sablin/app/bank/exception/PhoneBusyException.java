package ru.sablin.app.bank.client.exception;

public class PhoneBusyException extends RuntimeException {
    public PhoneBusyException(String message) {
        super(message);
    }
}
