package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;

@ResponseErrorCode("LOGIN_FAILED")
public final class LoginException extends RuntimeException {
    public LoginException(final String message) {
        super(message);
    }

    public LoginException(final Throwable cause) {
        super(cause);
    }
}
