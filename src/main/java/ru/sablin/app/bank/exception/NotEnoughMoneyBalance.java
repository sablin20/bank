package ru.sablin.app.bank.exception;

public class NotEnoughMoneyBalance extends RuntimeException {
    public NotEnoughMoneyBalance(String message) {
        super(message);
    }
}
