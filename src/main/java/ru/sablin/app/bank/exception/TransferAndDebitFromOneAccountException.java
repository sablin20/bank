package ru.sablin.app.bank.exception;

public class TransferAndDebitFromOneAccountException extends RuntimeException {
    public TransferAndDebitFromOneAccountException(String message) {
        super(message);
    }
}
