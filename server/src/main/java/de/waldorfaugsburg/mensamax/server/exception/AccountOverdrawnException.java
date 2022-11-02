package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;

@ResponseErrorCode("ACCOUNT_OVERDRAWN")
public final class AccountOverdrawnException extends RuntimeException {
}
