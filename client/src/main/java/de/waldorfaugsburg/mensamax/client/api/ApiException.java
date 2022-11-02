package de.waldorfaugsburg.mensamax.client.api;

import lombok.Getter;

@Getter
public final class ApiException extends RuntimeException {

    private int responseCode;
    private ApiError error;

    public ApiException(final Throwable cause) {
        super(cause);
    }

    public ApiException(final int responseCode, final ApiError error) {
        super(error.toString());
        this.responseCode = responseCode;
        this.error = error;
    }

    @Override
    public String toString() {
        return "ApiException{" +
                "responseCode=" + responseCode +
                ", error=" + error +
                '}';
    }
}
