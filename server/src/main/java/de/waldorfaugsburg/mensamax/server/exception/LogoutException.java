package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;

@ResponseErrorCode("LOGOUT_FAILED")
public final class LogoutException extends RuntimeException {

    public LogoutException(final String message) {
        super(message);
    }

    public LogoutException(final Throwable cause) {
        super(cause);
    }
}
