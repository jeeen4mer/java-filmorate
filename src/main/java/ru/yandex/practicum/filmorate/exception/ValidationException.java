package ru.yandex.practicum.filmorate.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(final String message) {
        super(message);
        this.errors = List.of(message);
    }
}