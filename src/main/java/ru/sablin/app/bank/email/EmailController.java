package ru.sablin.app.bank.email;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/emails")
public class EmailController {

    private final EmailService service;

    @DeleteMapping("/{email}")
    public void remove(@PathVariable("email") String email) {
        service.delete(email);
    }

    @PostMapping("/")
    public void create(@RequestBody Email email) {
        service.create(email.getClientId(), email.getEmail());
    }
}