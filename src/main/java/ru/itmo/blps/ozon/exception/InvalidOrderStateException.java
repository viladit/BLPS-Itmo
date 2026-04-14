package ru.itmo.blps.ozon.exception;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
