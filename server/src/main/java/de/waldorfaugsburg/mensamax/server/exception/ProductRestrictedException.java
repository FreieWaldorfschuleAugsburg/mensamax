package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;

@ResponseErrorCode("PRODUCT_RESTRICTED")
public final class ProductRestrictedException extends RuntimeException {
}
