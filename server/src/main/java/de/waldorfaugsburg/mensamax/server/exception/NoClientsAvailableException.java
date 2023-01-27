package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;

@ResponseErrorCode("NO_CLIENTS_AVAILABLE")
public final class NoClientsAvailableException extends RuntimeException {
    public NoClientsAvailableException() {
        super("Could not find an unused selenium client");
    }
}
