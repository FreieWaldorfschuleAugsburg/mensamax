package de.waldorfaugsburg.mensamax.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@AllArgsConstructor
@Data
@ToString
public final class MensaMaxTransaction {

    private long id;
    private String chip;
    private long barcode;
    private int quantity;
    private TransactionStatus status;
    private Date recordedAt;
    private Date performedAt;
}