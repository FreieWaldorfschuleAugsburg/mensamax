package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;

@ResponseErrorCode("LOGIN_FAILED")
public final class LoginException extends RuntimeException {

    public LoginException() {
        super("Login has failed");
    }
}
