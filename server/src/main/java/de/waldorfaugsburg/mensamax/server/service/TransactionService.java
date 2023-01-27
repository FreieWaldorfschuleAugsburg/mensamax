package de.waldorfaugsburg.mensamax.server.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.waldorfaugsburg.mensamax.server.exception.TransactionNotFoundException;
import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TransactionService {
    public final Cache<String, MensaMaxTransaction> transactionCache = CacheBuilder.newBuilder().build();
    public final Cache<String, String> userTransactionCache = CacheBuilder.newBuilder().build();

    public void logTransaction(MensaMaxTransaction mensaMaxTransaction, String chip) {
        transactionCache.put(mensaMaxTransaction.getTransactionId(), mensaMaxTransaction);
        userTransactionCache.put(mensaMaxTransaction.getTransactionId(), chip);
    }

    public MensaMaxTransaction getTransaction(String transactionId) {
        try {
            return transactionCache.getIfPresent(transactionId);
        } catch (final Exception e) {
            throw new TransactionNotFoundException();
        }
    }

    public Set<MensaMaxTransaction> getSuspiciousTransactions() {
        final Set<MensaMaxTransaction> suspiciousTransactions = new HashSet<>();
        final Collection<String> chipIds = userTransactionCache.asMap().values();
        final Set<String> duplicateChipIds = getDuplicateChipIds(chipIds);
        for (String chipId : duplicateChipIds) {
            Set<MensaMaxTransaction> transactions = getTransactionsForChipId(chipId);
            LocalDateTime lastDate = null;
            for (MensaMaxTransaction transaction : transactions) {
                if (lastDate != null) {
                    Duration delta = Duration.between(transaction.getDate(), lastDate);
                    if (delta.toSeconds() < 30) {
                        suspiciousTransactions.add(transaction);
                    }
                }
                lastDate = transaction.getDate();

            }
        }
        return suspiciousTransactions;
    }

    private Set<String> getDuplicateChipIds(Collection<String> chipIds) {
        Set<String> uniqueIds = new HashSet<>();
        return chipIds.stream()
                .filter(chipId -> !uniqueIds.add(chipId))
                .collect(Collectors.toSet());
    }

    private Set<MensaMaxTransaction> getTransactionsForChipId(String chipId) {
        Set<MensaMaxTransaction> transactions = new HashSet<>();
        Stream<String> transactionStream = userTransactionCache
                .asMap()
                .entrySet()
                .stream()
                .filter(entry -> chipId.equals(entry.getValue()))
                .map(Map.Entry::getKey);

        transactionStream.forEach(transactionId -> {
            MensaMaxTransaction transaction = transactionCache.getIfPresent(transactionId);
            transactions.add(transaction);
        });
        return transactions;
    }


}