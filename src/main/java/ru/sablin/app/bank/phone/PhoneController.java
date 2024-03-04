package ru.sablin.app.bank.phone;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/phones")
public class PhoneController {

    private final PhoneService service;

    @DeleteMapping("/{phone}")
    public void remove(@PathVariable("phone") String phone) {
        service.delete(phone);
    }

    @PostMapping("/")
    public void create(@RequestBody Phone phone) {
        service.create(phone.getClientId(), phone.getPhone());
    }
}