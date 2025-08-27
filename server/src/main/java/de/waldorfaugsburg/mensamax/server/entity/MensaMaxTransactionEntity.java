package de.waldorfaugsburg.mensamax.server.entity;

import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import de.waldorfaugsburg.mensamax.transaction.TransactionStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.Instant;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = {"id"})
public class MensaMaxTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String username;
    private String chip;
    private String kiosk;
    private long barcode;
    private int quantity;
    private TransactionStatus status;

    private Instant recordedAt;
    @Nullable
    private Instant performedAt;

    public MensaMaxTransaction asModel() {
        return new MensaMaxTransaction(id, username, chip, barcode, quantity, status, Date.from(recordedAt), performedAt != null ? Date.from(performedAt) : null);
    }
}
