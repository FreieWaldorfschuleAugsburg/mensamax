package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseErrorCode("INVALID_PRODUCT")
@ResponseStatus(HttpStatus.NOT_FOUND)
public final class InvalidProductException extends RuntimeException {

    private final long productBarcode;
    private final String kiosk;

    public InvalidProductException(final long productBarcode, final String kiosk) {
        super(String.format("Could not find product with barcode %s in kiosk %s", productBarcode, kiosk));
        this.productBarcode = productBarcode;
        this.kiosk = kiosk;
    }

    @ResponseErrorProperty
    public long getProductBarcode() {
        return productBarcode;
    }

    @ResponseErrorProperty
    public String getKiosk() {
        return kiosk;
    }
}
