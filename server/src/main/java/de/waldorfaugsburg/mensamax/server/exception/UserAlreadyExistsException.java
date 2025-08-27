package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorProperty;

@ResponseErrorCode("USER_ALREADY_EXISTS")
public final class UserAlreadyExistsException extends RuntimeException {

    private final String username;

    public UserAlreadyExistsException(final String username) {
        super(String.format("User %s already exists", username));
        this.username = username;
    }

    public UserAlreadyExistsException(final String username, final Throwable cause) {
        super(String.format("User %s already exists", username), cause);
        this.username = username;
    }

    @ResponseErrorProperty
    public String getUsername() {
        return username;
    }
}
