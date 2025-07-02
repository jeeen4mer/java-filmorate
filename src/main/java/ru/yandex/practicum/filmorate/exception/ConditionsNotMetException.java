package ru.yandex.practicum.filmorate.exception;

public class ConditionsNotMetException extends RuntimeException {
    public ConditionsNotMetException(final String message) {
        super(message);
    }
}