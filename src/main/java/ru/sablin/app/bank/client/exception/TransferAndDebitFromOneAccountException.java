package ru.sablin.app.bank.client.exception;

public class TransferAndDebitFromOneAccountException extends RuntimeException {
    public TransferAndDebitFromOneAccountException(String message) {
        super(message);
    }
}
