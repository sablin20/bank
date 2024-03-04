package ru.sablin.app.bank.email;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.sablin.app.bank.exception.ClientNotFoundException;
import ru.sablin.app.bank.exception.EmailBusyException;
import ru.sablin.app.bank.exception.EmailException;
import ru.sablin.app.bank.exception.EmailLastException;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JdbcTemplate jdbcTemplate;
    private final EmailRepository repository;

    public void create(long clientId, String email) {
        validEmail(List.of(email));

        // проверяем что есть клиент с таким id
        try {
            var idClient = jdbcTemplate.queryForObject("select id from Client where id = ?",
                    Integer.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            throw new ClientNotFoundException(String.format("Client with id = %s is not found", clientId));
        }

        // проверяем что почта свободна
        var listEmail = jdbcTemplate.queryForList("select email from Email", String.class);
        if (listEmail.contains(email)) {
            throw new EmailBusyException(String.format("Email = %s is busy", email));
        }

        repository.create(clientId, email);
    }

    public void delete(String email) {
        validEmail(List.of(email));

        var list = jdbcTemplate.queryForList("""
            select email
            from Email
            where client_id =
                (select client_id
                from Email
                where email = ?)
            """,
                String.class, email);

        // проверяем что почта есть в базе
        if (!list.contains(email)) {
            throw new EmailException(String.format("Email = %s not found", email));
        }
        // проверяем что email не последний
        if (list.size() <= 1) {
            throw new EmailLastException(String.format("This email = %s is last", email));
        }

        repository.delete(email);
    }

    private void validEmail(List<String> email) {
        var pattern = Pattern.compile("\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*\\.\\w{2,4}");
        for (String e : email) {
            var matcher = pattern.matcher(e);
            if (!matcher.matches()) {
                throw new EmailException(String.format("%s this email is invalid", e));
            }
        }
    }
}