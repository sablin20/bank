package ru.sablin.app.bank.phone;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.sablin.app.bank.exception.ClientNotFoundException;
import ru.sablin.app.bank.exception.PhoneException;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PhoneRepositoryImpl implements PhoneRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void create(long clientId, String phone) {
        jdbcTemplate.update("insert into Phone values (?,?)", clientId, phone);
    }

    @Override
    public void delete(String phone) {
        jdbcTemplate.update("delete from Phone where phone = ?", phone);
    }

    @Override
    public List<String> findByPhone(String phone) {
        List<String> listPhones;
        try {
            listPhones = jdbcTemplate.queryForList("select phone from Phone where client_id = (select client_id from Phone where phone = ?)",
                    String.class, phone);
        } catch (EmptyResultDataAccessException e) {
            throw new PhoneException(String.format("Phone = %s is not found", phone));
        }
        return listPhones;
    }

    @Override
    public Integer findClientIdByPhone(String phone) {
        try {
            return jdbcTemplate.queryForObject("select client_id from Phone where phone = ?",
                    Integer.class, phone);
        } catch (EmptyResultDataAccessException e) {
            throw new PhoneException("Phone not found");
        }
    }

    @Override
    public List<String> findByClientId(Integer clientId) {
        List<String> listPhones;
        try {
            listPhones = jdbcTemplate.queryForList("select phone from Phone where client_id = ?",
                    String.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            throw new ClientNotFoundException(String.format("Client with id = %s is not found", clientId));
        }
        return listPhones;
    }
}
