package ru.sablin.app.bank.client;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@Getter
@Setter
public class ClientDto {
    Integer id;
    String fio;
    LocalDate birthday;
    List<String> phone;
    List<String> email;
}
