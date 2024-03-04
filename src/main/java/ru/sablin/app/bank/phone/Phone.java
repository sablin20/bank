package ru.sablin.app.bank.phone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Phone {
    private long clientId;
    private String phone;
}
