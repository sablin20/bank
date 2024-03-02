package ru.sablin.app.bank.client;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Client {
    Integer id;
    String fio;
    LocalDate birthday;
    List<String> phone;
    List<String> email;
    String login;
    String password;
    BigDecimal balance;
}
