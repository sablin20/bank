package ru.sablin.app.bank.client;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClientRepository {
    void create(Client client);

    void addEmail(Integer clientId, String email);

    void addPhone(Integer clientId, String phone);

    void removePhone(String phone);

    void removeEmail(String email);

    List<ClientDto> findByParam(LocalDate birthday,
                                String phone,
                                String fio,
                                String email);

    void increaseInBalance();

    void moneyTransfer(Integer clientIdSender,
                       Integer clientIdRecipient,
                       BigDecimal money);
}