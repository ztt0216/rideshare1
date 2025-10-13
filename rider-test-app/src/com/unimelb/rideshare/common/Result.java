package com.unimelb.rideshare.common;

import java.util.Objects;
import java.util.Optional;

/**
 * Lightweight result wrapper for service operations.
 */
public final class Result<T> {
    private final boolean success;
    private final T value;
    private final String message;

    private Result(boolean success, T value, String message) {
        this.success = success;
        this.value = value;
        this.message = message;
    }

    public static <T> Result<T> ok(T value) {
        return new Result<>(true, value, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(false, null, Objects.requireNonNullElse(message, "Unknown error"));
    }

    public boolean isSuccess() {
        return success;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }
}
