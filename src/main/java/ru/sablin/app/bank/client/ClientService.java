package ru.sablin.app.bank.client;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.sablin.app.bank.client.exception.*;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepositoryImpl repository;
    private final JdbcTemplate jdbcTemplate;

    public void create(Client client) {

        // проверяем валидность почты и телефона
        validEmail(client.getEmail());
        validPhone(client.getPhone());

        // проверяем что логин свободен
        var loginInDb = jdbcTemplate.query("SELECT login FROM Client",
                new BeanPropertyRowMapper<>(String.class));

        if (!loginInDb.isEmpty()) {
            for (String s : loginInDb) {
                if (s.equals(client.getLogin())) {
                    throw new LoginBusyException(String.format("Login = %s, busy", client.getLogin()));
                }
            }
        }

        // проверяем что телефон свободен
        var phoneInDb = jdbcTemplate.query("SELECT phone FROM Phone",
                new BeanPropertyRowMapper<>(String.class));

        if (!phoneInDb.isEmpty()) {
            for (String p : phoneInDb) {
                for (String ph : client.getPhone()) {
                    if (p.equals(ph)) {
                        throw new PhoneBusyException(String.format("Phone = %s, busy", ph));
                    }
                }
            }
        }

        // проверяем что почта свободна
        var emailInDb = jdbcTemplate.query("SELECT email FROM Email",
                new BeanPropertyRowMapper<>(String.class));

        if (!emailInDb.isEmpty()) {
            for (String e : emailInDb) {
                for (String em : client.getEmail()) {
                    if (e.equals(em)) {
                        throw new EmailBusyException(String.format("Email = %s, busy", em));
                    }
                }
            }
        }

        repository.create(client);
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
    private void validPhone(List<String> phone) {
        var number = "\\+7\\d{10}";
        for (String p : phone) {
            if (!p.matches(number)) {
                throw new PhoneException(String.format("%s this phone is invalid", p));
            }
        }
    }

    public void addEmail(Integer clientId, String email) {}

}