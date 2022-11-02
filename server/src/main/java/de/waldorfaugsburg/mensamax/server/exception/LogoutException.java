package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;

@ResponseErrorCode("LOGOUT_FAILED")
public final class LogoutException extends RuntimeException {

    public LogoutException() {
        super("Logout has failed");
    }
}
