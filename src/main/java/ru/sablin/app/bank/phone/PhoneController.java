package ru.sablin.app.bank.phone;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/phones")
public class PhoneController {

    private final PhoneService service;

    @DeleteMapping("/")
    public void remove(@RequestParam("phone") String phone) {
        service.delete(phone);
    }

    @PostMapping("/")
    public void create(@RequestBody Phone phone) {
        service.create(phone.getClientId(), phone.getPhone());
    }

    @GetMapping("/")
    public List<String> getByPhone(@RequestParam("phone") String phone) {
        return service.findByPhone(phone);
    }

    @GetMapping("/getClientId")
    public Integer getClientIdByPhone(@RequestParam("phone") String phone) {
        return service.findClientIdByPhone(phone);
    }

    @GetMapping("/byClientId")
    public List<String> getByClientId(@RequestParam("clientId") Integer clientId) {
       return service.findByClientId(clientId);
    }
}