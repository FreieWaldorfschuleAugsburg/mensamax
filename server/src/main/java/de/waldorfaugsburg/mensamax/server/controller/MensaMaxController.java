package de.waldorfaugsburg.mensamax.server.controller;

import de.waldorfaugsburg.mensamax.common.MensaMaxUser;
import de.waldorfaugsburg.mensamax.server.service.MensaMaxService;
import de.waldorfaugsburg.mensamax.server.service.TransactionService;
import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class MensaMaxController {
    private final MensaMaxService mensaMaxService;
    private final TransactionService transactionService;

    public MensaMaxController(final MensaMaxService service, final TransactionService transactionService) {
        this.transactionService = transactionService;
        this.mensaMaxService = service;
    }

    @GetMapping("/user/{chip}")
    public ResponseEntity<MensaMaxUser> getUserByChipId(@PathVariable("chip") final String chip) {
        final MensaMaxUser user = mensaMaxService.getUserByChip(chip);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("/transaction")
    public ResponseEntity<Void> transaction(@RequestParam("id") final UUID id, @RequestParam("chip") final String chip, @RequestParam("kiosk") final String kiosk, @RequestParam("barcode") final long barcode, @RequestParam("quantity") final int quantity) {
        mensaMaxService.transaction(id, chip, kiosk, barcode, quantity);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/transaction/{chip}/last")
    public ResponseEntity<MensaMaxTransaction> getLastTransactionByChip(@PathVariable("chip") final String chip) {
        final MensaMaxTransaction transaction = transactionService.getLastTransaction(chip);
        return new ResponseEntity<>(transaction, HttpStatus.OK);
    }

    @GetMapping("/transaction/{chip}/all")
    public ResponseEntity<List<MensaMaxTransaction>> getTransactionsByChip(@PathVariable("chip") final String chip) {
        final List<MensaMaxTransaction> transactions = transactionService.getTransactions(chip);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }
}
