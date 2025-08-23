package de.waldorfaugsburg.mensamax.server.repository;

import de.waldorfaugsburg.mensamax.server.entity.MensaMaxTransactionEntity;
import de.waldorfaugsburg.mensamax.transaction.TransactionStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface TransactionRepository extends CrudRepository<MensaMaxTransactionEntity, Long> {

    List<MensaMaxTransactionEntity> findAllByStatus(final TransactionStatus status);

    Stream<MensaMaxTransactionEntity> streamAllByStatus(final TransactionStatus status);

}
