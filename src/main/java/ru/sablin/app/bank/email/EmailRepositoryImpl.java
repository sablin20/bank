package ru.sablin.app.bank.email;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.sablin.app.bank.exception.ClientNotFoundException;
import ru.sablin.app.bank.exception.EmailBusyException;
import ru.sablin.app.bank.exception.EmailException;
import ru.sablin.app.bank.exception.PhoneException;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EmailRepositoryImpl implements EmailRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void create(long clientId, String email) {
        jdbcTemplate.update("insert into Email values (?,?)", clientId, email);
    }

    @Override
    public void delete(String email) {
        jdbcTemplate.update("delete from Email where email = ?", email);
    }

    @Override
    public List<String> findByClientId(Integer clientId) {
        List<String> listEmail;
        try {
            listEmail = jdbcTemplate.queryForList("select email from Email where client_id = ?",
                    String.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            throw new ClientNotFoundException(String.format("Client with id = %s is not found", clientId));
        }
        return listEmail;
    }

    @Override
    public Integer findClientIdByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject("select client_id from Email where email = ?",
                    Integer.class, email);
        } catch (EmptyResultDataAccessException e) {
            throw new EmailException(String.format("Email = %s is not found", email));
        }
    }

    @Override
    public List<String> findByEmail(String email) {
        List<String> listEmails;
        try {
            listEmails = jdbcTemplate.queryForList("select email from Email where client_id = (select client_id from Email where email = ?)",
                    String.class, email);
        } catch (EmptyResultDataAccessException e) {
            throw new EmailException(String.format("Email = %s is not found", email));
        }
        return listEmails;
    }
}
