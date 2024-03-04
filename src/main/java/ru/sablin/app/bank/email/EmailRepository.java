package ru.sablin.app.bank.email;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailRepository {
    void create(long clientId, String email);
    void delete(String email);
}
