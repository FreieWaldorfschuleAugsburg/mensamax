package de.waldorfaugsburg.mensamax.transaction;

import de.waldorfaugsburg.mensamax.common.MensaMaxUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@AllArgsConstructor
@Data
@ToString
public final class MensaMaxTransaction {
    private String transactionId;
    private MensaMaxUser mensaMaxUser;
    private long barcode;
    private TransactionStatus transactionStatus;
    private LocalDateTime date;
}