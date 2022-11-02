package de.waldorfaugsburg.mensamax.server.exception;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorCode;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ResponseErrorProperty;

@ResponseErrorCode("NO_STOCK")
public final class NoStockException extends RuntimeException {

    private final long productBarcode;
    private final String kiosk;

    public NoStockException(final long productBarcode, final String kiosk) {
        super(String.format("Product with barcode %s is out of stock in kiosk %s", productBarcode, kiosk));
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
