package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorProperty;

@ResponseErrorCode("USER_CREATION")
public final class UserCreationException extends RuntimeException {

    private final String username;

    public UserCreationException(final String username) {
        super(String.format("Could not create user %s", username));
        this.username = username;
    }

    public UserCreationException(final String username, final Throwable cause) {
        super(String.format("Could not create user %s", username), cause);
        this.username = username;
    }

    @ResponseErrorProperty
    public String getUsername() {
        return username;
    }
}
