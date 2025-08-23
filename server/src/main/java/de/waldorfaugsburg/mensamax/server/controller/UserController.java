package de.waldorfaugsburg.mensamax.server.controller;

import de.waldorfaugsburg.mensamax.common.MensaMaxUser;
import de.waldorfaugsburg.mensamax.server.service.MensaMaxService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final MensaMaxService mensaMaxService;

    public UserController(final MensaMaxService service) {
        this.mensaMaxService = service;
    }

    @GetMapping("/user/username/{username}")
    public ResponseEntity<MensaMaxUser> getUserByChipId(@PathVariable("username") final String username) {
        final MensaMaxUser user = mensaMaxService.getUserByUsername(username);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/user/employeeId/{employeeId}")
    public ResponseEntity<MensaMaxUser> getUserByChipId(@PathVariable("employeeId") final int employeeId) {
        final MensaMaxUser user = mensaMaxService.getUserByEmployeeId(employeeId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}
