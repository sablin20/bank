package ru.sablin.app.bank.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Email {
    private long clientId;
    private String email;
}
