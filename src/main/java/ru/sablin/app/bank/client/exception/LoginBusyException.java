package ru.sablin.app.bank.client.exception;

public class LoginBusyException extends RuntimeException {
    public LoginBusyException(String message) {
        super(message);
    }
}
