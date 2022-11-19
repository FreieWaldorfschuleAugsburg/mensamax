package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorProperty;

@ResponseErrorCode("INVALID_CHIP")
public final class InvalidChipException extends RuntimeException {

    private final String chip;

    public InvalidChipException(final String chip) {
        super(String.format("Could not find user with chip %s", chip));
        this.chip = chip;
    }

    public InvalidChipException(final String chip, final String message) {
        super(String.format("Could not find user with chip %s: %s", chip, message));
        this.chip = chip;
    }

    public InvalidChipException(final String chip, final Throwable cause) {
        super(String.format("Could not find user with chip %s", chip), cause);
        this.chip = chip;
    }

    @ResponseErrorProperty
    public String getChip() {
        return chip;
    }
}
