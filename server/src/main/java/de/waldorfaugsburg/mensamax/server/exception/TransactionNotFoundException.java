package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseErrorCode("TRANSACTION_NOT_FOUND")
@ResponseStatus(HttpStatus.NOT_FOUND)
public final class TransactionNotFoundException extends RuntimeException {
}
