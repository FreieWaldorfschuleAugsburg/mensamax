package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;

@ResponseErrorCode("UNKNOWN_ERROR")
public final class UnknownErrorException extends RuntimeException {

    public UnknownErrorException(final Throwable cause) {
        super(cause);
    }

    public UnknownErrorException(final String message) {
        super(message);
    }
}
