package ru.sablin.app.bank.client;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClientRepository {
    void create(Client client);

    void addEmail(Integer clientId, String email);
    void addPhone(Integer clientId, String phone);
    // скорее всего не нужен id для удаления
    void removePhone(Integer clientId, String phone);
    void removeEmail(Integer clientId, String email);

    List<ClientDto> findByParam(LocalDate birthday,
                                String phone,
                                String fio,
                                String email);
}
