package de.waldorfaugsburg.mensamax.server.service;

import com.google.common.collect.ArrayListMultimap;
import de.waldorfaugsburg.mensamax.server.exception.TransactionNotFoundException;
import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TransactionService {

    public final ArrayListMultimap<String, MensaMaxTransaction> transactionCache = ArrayListMultimap.create();

    public void saveTransaction(final MensaMaxTransaction mensaMaxTransaction) {
        transactionCache.put(mensaMaxTransaction.getChip(), mensaMaxTransaction);
    }

    public MensaMaxTransaction getLastTransaction(final String chipId) {
        final List<MensaMaxTransaction> transactions = getTransactions(chipId);
        if (transactions.isEmpty()) {
            throw new TransactionNotFoundException();
        }

        final MensaMaxTransaction lastTransaction = transactions.get(transactions.size() - 1);
        if (lastTransaction == null) {
            throw new TransactionNotFoundException();
        }

        return lastTransaction;
    }

    public List<MensaMaxTransaction> getTransactions(final String chipId) {
        return transactionCache.get(chipId);
    }
}