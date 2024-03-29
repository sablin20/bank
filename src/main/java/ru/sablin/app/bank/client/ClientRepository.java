package ru.sablin.app.bank.client;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClientRepository {
    void create(Client client);

    List<Client> findByParams(LocalDate birthday,
                              String phone,
                              String fio,
                              String email);

    void increaseInBalance();

    void moneyTransfer(long clientIdSender,
                       long clientIdRecipient,
                       BigDecimal money);
}