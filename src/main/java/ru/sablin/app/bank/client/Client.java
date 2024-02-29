package ru.sablin.app.bank.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
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