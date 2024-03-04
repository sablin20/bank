package ru.sablin.app.bank.client;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService service;

    @PostMapping("/")
    public void create(@RequestBody Client client) {
        service.create(client);
    }

    @GetMapping("/")
    public List<Client> find(@RequestParam(value = "birthday", required = false) LocalDate birthday,
                             @RequestParam(value = "phone", required = false) String phone,
                             @RequestParam(value = "fio", required = false) String fio,
                             @RequestParam(value = "email", required = false) String email) {

        return service.getByParams(birthday, phone, fio, email);
    }

    @PutMapping("/")
    public void increaseInBalance() {
        service.increaseInBalance();
    }

    @PutMapping("/transfer")
    public void moneyTransfer(@RequestParam("clientIdSender") long clientIdSender,
                              @RequestParam("clientIdRecipient") long clientIdRecipient,
                              @RequestParam("money") BigDecimal money) {
        service.moneyTransfer(clientIdSender, clientIdRecipient, money);
    }
}
