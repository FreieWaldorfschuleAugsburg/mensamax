package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;

@ResponseErrorCode("TRANSACTION_NOT_FOUND")
public final class TransactionNotFoundException extends RuntimeException {
}
