package ru.sablin.app.bank.client.exception;

public class NotEnoughMoneyBalance extends RuntimeException {
    public NotEnoughMoneyBalance(String message) {
        super(message);
    }
}
