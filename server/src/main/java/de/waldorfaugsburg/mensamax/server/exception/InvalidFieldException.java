package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseErrorCode("INVALID_FIELD")
@ResponseStatus(HttpStatus.NOT_FOUND)
public final class InvalidFieldException extends RuntimeException {

    private final String inputFieldName;
    private final String value;

    public InvalidFieldException(final String inputFieldName, final String value) {
        super(String.format("Could not find user by field %s with value %s", inputFieldName, value));
        this.inputFieldName = inputFieldName;
        this.value = value;
    }

    public InvalidFieldException(final String inputFieldName, final String value, final Throwable cause) {
        super(String.format("Could not find user by field %s with value %s", inputFieldName, value), cause);
        this.inputFieldName = inputFieldName;
        this.value = value;
    }

    @ResponseErrorProperty
    public String getValue() {
        return value;
    }

    @ResponseErrorProperty
    public String getInputFieldName() {
        return inputFieldName;
    }
}
