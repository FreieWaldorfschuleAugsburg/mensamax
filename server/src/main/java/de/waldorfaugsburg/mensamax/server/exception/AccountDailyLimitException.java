package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;

@ResponseErrorCode("ACCOUNT_DAILY_LIMIT")
public final class AccountDailyLimitException extends RuntimeException {
}
