package ru.sablin.app.bank.exception;

public class PhoneBusyException extends RuntimeException {
    public PhoneBusyException(String message) {
        super(message);
    }
}
