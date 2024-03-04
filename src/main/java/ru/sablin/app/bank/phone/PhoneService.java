package ru.sablin.app.bank.phone;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.sablin.app.bank.exception.ClientNotFoundException;
import ru.sablin.app.bank.exception.PhoneBusyException;
import ru.sablin.app.bank.exception.PhoneException;
import ru.sablin.app.bank.exception.PhoneLastException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PhoneService {

    private final PhoneRepository repository;

    private final JdbcTemplate jdbcTemplate;

    public void create(long clientId, String phone) {
        validPhone(List.of(phone));

        // проверяем что есть клиент с таким id
        try {
            var idClient = jdbcTemplate.queryForObject("select id from Client where id = ?",
                    Integer.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            throw new ClientNotFoundException(String.format("Client with id = %s is not found", clientId));
        }

        // проверяем что телефон свободен
        var listPhone = jdbcTemplate.queryForList("select phone from Phone", String.class);
        if (listPhone.contains(phone)) {
            throw new PhoneBusyException(String.format("Phone = %s is busy", phone));
        }

        repository.create(clientId, phone);
    }

    public void delete(String phone) {
        validPhone(List.of(phone));

        var list = jdbcTemplate.queryForList("""
                select phone
                from Phone
                where client_id =
                    (select client_id
                    from Phone
                    where phone = ?)
                """,
                String.class, phone);

        // проверяем что phone есть в базе
        if (!list.contains(phone)) {
            throw new PhoneException(String.format("Phone = %s not found", phone));
        }

        // проверяем что телефон не последний
        if (list.size() <= 1) {
            throw new PhoneLastException(String.format("This phone = %s is last", phone));
        }

        repository.delete(phone);
    }

    public List<String> findByPhone(String phone) {
        return repository.findByPhone(phone);
    }

    public Integer findClientIdByPhone(String phone) {
        return repository.findClientIdByPhone(phone);
    }

    public List<String> findByClientId(Integer clientId) {
        return repository.findByClientId(clientId);
    }

    private void validPhone(List<String> phone) {
        var number = "8\\d{10}";
        for (String p : phone) {
            if (!p.matches(number)) {
                throw new PhoneException(String.format("%s this phone is invalid", p));
            }
        }
    }
}