package ru.sablin.app.bank.email;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/emails")
public class EmailController {

    private final EmailService service;

    @DeleteMapping("/")
    public void remove(@RequestParam("email") String email) {
        service.delete(email);
    }

    @PostMapping("/")
    public void create(@RequestBody Email email) {
        service.create(email.getClientId(), email.getEmail());
    }

    @GetMapping("/")
    public List<String> getByEmail(@RequestParam("email") String email) {
        return service.findByEmail(email);
    }

    @GetMapping("/getClientId")
    public Integer getClientIdByEmail(@RequestParam("email") String email) {
        return service.findClientIdByEmail(email);
    }

    @GetMapping("/byClientId")
    public List<String> getByClientId(@RequestParam("clientId") Integer clientId) {
        return service.findByClientId(clientId);
    }
}