package ru.sablin.app.bank.phone;

import org.springframework.stereotype.Repository;

@Repository
public interface PhoneRepository {
    void create(long clientId, String phone);
    void delete(String phone);
}
