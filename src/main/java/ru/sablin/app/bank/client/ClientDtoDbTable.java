package ru.sablin.app.bank.client;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Getter
@Setter
public class ClientDtoDbTable {
    Integer id;
    String fio;
    LocalDate birthday;
    String login;
    String password;
    BigDecimal balance;
}