package ru.sablin.app.bank.exception;

public class LoginBusyException extends RuntimeException {
    public LoginBusyException(String message) {
        super(message);
    }
}
