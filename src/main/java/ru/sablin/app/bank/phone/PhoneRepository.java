package ru.sablin.app.bank.phone;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhoneRepository {
    void create(long clientId, String phone);
    void delete(String phone);

    List<String> findByPhone(String phone);
    Integer findClientIdByPhone(String phone);
    List<String> findByClientId(Integer clientId);
}
