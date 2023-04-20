package de.waldorfaugsburg.mensamax.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.UUID;

@AllArgsConstructor
@Data
@ToString
public final class MensaMaxTransaction {
    private UUID id;
    private String chip;
    private long barcode;
    private long timestamp;
}