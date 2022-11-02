package de.waldorfaugsburg.mensamax.server.controller;

import de.waldorfaugsburg.mensamax.common.MensaMaxUser;
import de.waldorfaugsburg.mensamax.server.service.MensaMaxService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class MensaMaxController {

    private final MensaMaxService service;

    public MensaMaxController(final MensaMaxService service) {
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
                                            @RequestParam("quantity") final int quantity) {
        service.transaction(chip, kiosk, barcode, quantity);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
