package ru.sablin.app.bank.email;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.sablin.app.bank.exception.ClientNotFoundException;
import ru.sablin.app.bank.exception.EmailBusyException;

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
}
