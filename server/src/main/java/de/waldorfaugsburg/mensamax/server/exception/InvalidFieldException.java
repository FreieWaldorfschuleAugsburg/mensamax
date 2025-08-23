package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseErrorCode("INVALID_FIELD")
@ResponseStatus(HttpStatus.NOT_FOUND)
public final class InvalidFieldException extends RuntimeException {

    private final String username;

    public InvalidFieldException(final String inputFieldName, final String username) {
        super(String.format("Could not find user by field %s with value %s", inputFieldName, username));
        this.username = username;
    }

    public InvalidFieldException(final String inputFieldName, final String username, final Throwable cause) {
        super(String.format("Could not find user by field %s with value %s", inputFieldName, username), cause);
        this.username = username;
    }

    @ResponseErrorProperty
    public String getUsername() {
        return username;
    }
}
