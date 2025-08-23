package de.waldorfaugsburg.mensamax.server.controller;

import de.waldorfaugsburg.mensamax.server.entity.MensaMaxTransactionEntity;
import de.waldorfaugsburg.mensamax.server.service.TransactionService;
import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import de.waldorfaugsburg.mensamax.transaction.TransactionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(final TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/transactions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MensaMaxTransaction>> fetchAllTransactions() {
        return new ResponseEntity<>(transactionService.fetchAllTransactions(), HttpStatus.OK);
    }

    @GetMapping("/transaction/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<MensaMaxTransaction> fetchTransactionById(final @PathVariable(name = "id") int id) {
        return new ResponseEntity<>(transactionService.fetchTransactionById(id), HttpStatus.OK);
    }

    @GetMapping("/transactions/{status}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MensaMaxTransaction>> fetchAllTransactionsByStatus(final @PathVariable(name = "status") TransactionStatus status) {
        return new ResponseEntity<>(transactionService.fetchAllTransactionsByStatus(status), HttpStatus.OK);
    }

    @DeleteMapping("/transaction/{id}")
    public ResponseEntity<Void> deleteTransactionById(final @PathVariable(name = "id") int id) {
        transactionService.deleteTransactionById(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/transaction")
    public ResponseEntity<Void> recordTransaction(@RequestParam("chip") final String chip, @RequestParam("kiosk") final String kiosk, @RequestParam("barcode") final long barcode, @RequestParam("quantity") final int quantity) {
        transactionService.recordTransaction(chip, kiosk, barcode, quantity);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
