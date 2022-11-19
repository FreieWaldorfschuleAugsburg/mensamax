package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorProperty;

@ResponseErrorCode("INVALID_USERNAME")
public final class InvalidUsernameException extends RuntimeException {

    private final String username;

    public InvalidUsernameException(final String username) {
        super(String.format("Could not find user with username %s", username));
        this.username = username;
    }

    public InvalidUsernameException(final String username, final Throwable cause) {
        super(String.format("Could not find user with username %s", username), cause);
        this.username = username;
    }

    @ResponseErrorProperty
    public String getUsername() {
        return username;
    }
}
