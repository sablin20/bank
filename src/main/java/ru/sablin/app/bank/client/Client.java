package ru.sablin.app.bank.client;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class Client {
    private Integer id;
    private String fio;
    private LocalDate birthday;
    private List<String> phone;
    private List<String> email;
    private String login;
    // поставить аннотацию, что пароль не надо отправлять, либо убрать
    private String password;
    private BigDecimal balance;
}