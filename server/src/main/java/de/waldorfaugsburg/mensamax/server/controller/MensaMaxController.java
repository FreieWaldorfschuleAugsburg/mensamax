package de.waldorfaugsburg.mensamax.server.controller;

import de.waldorfaugsburg.mensamax.common.MensaMaxUser;
import de.waldorfaugsburg.mensamax.server.service.MensaMaxService;
import de.waldorfaugsburg.mensamax.server.service.TransactionService;
import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class MensaMaxController {

    private final MensaMaxService service;
    private final TransactionService transactionService;

    public MensaMaxController(final MensaMaxService service, final TransactionService transactionService) {
        this.transactionService = transactionService;
        this.service = service;
    }

    @GetMapping("/user/chip/{chip}")
    public ResponseEntity<MensaMaxUser> getUserByChipId(@PathVariable("chip") final String chip) {
        final MensaMaxUser user = service.getUserByChip(chip);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/user/username/{username}")
    public ResponseEntity<MensaMaxUser> getUserByUsername(@PathVariable("username") final String username) {
        final MensaMaxUser user = service.getUserByUsername(username);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("/transaction")
    public ResponseEntity<Void> transaction(@RequestParam("chip") final String chip,
                                            @RequestParam("kiosk") final String kiosk,
                                            @RequestParam("barcode") final long barcode,
                                            @RequestParam("quantity") final int quantity,
                                            @RequestParam("id") final String transactionId) {
        MensaMaxTransaction transaction = service.transaction(transactionId, chip, kiosk, barcode, quantity);
        transactionService.logTransaction(transaction, chip);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/transaction/{id}/status")
    public ResponseEntity<MensaMaxTransaction> getTransactionByTransactionId(@PathVariable("id") final String transactionId) {
        final MensaMaxTransaction transaction = transactionService.getTransaction(transactionId);
        return new ResponseEntity<>(transaction, HttpStatus.OK);

    }


}
