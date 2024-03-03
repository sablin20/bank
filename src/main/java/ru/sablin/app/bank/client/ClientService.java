package ru.sablin.app.bank.client;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.sablin.app.bank.client.exception.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public void create(Client client) {
        // проверяем валидность почты и телефона
        validEmail(client.getEmail());
        validPhone(client.getPhone());

        // проверяем, что стартовый баланс == текущему балансу на момент создания
        if (!client.getStartBalance().equals(client.getBalance())) {
            throw new BalanceInvalidException(String.format("Start balance = %s not equals current balance = %s",
                    client.getStartBalance(), client.getBalance()));
        }

        // проверяем что логин свободен
        var logins = jdbcTemplate.queryForList("SELECT login FROM Client",
                String.class);

        if (!logins.isEmpty()) {
            for (String s : logins) {
                if (s.equals(client.getLogin())) {
                    throw new LoginBusyException(String.format("Login = %s, busy", client.getLogin()));
                }
            }
        }

        // проверяем что телефон свободен
        var phones = jdbcTemplate.queryForList("SELECT phone FROM Phone",
                String.class);

        if (!phones.isEmpty()) {
            for (String p : phones) {
                for (String ph : client.getPhone()) {
                    if (p.equals(ph)) {
                        throw new PhoneBusyException(String.format("Phone = %s, busy", ph));
                    }
                }
            }
        }

        // проверяем что почта свободна
        var emails = jdbcTemplate.queryForList("SELECT email FROM Email",
                String.class);

        if (!emails.isEmpty()) {
            for (String e : emails) {
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
        var number = "8\\d{10}";
        for (String p : phone) {
            if (!p.matches(number)) {
                throw new PhoneException(String.format("%s this phone is invalid", p));
            }
        }
    }

    public List<Client> getByParams(LocalDate birthday,
                                    String phone,
                                    String fio,
                                    String email) {
        return repository.findByParams(birthday, phone, fio, email);
    }

    public void addEmail(long clientId, String email) {
        validEmail(List.of(email));
        repository.addEmail(clientId, email);
    }

    public void addPhone(long clientId, String phone) {
        validPhone(List.of(phone));
        repository.addPhone(clientId, phone);
    }

    public void removePhone(String phone) {
        validPhone(List.of(phone));
        repository.removePhone(phone);
    }

    public void removeEmail(String email) {
        validEmail(List.of(email));
        repository.removeEmail(email);
    }

    public void increaseInBalance() {
        repository.increaseInBalance();
    }

    public void moneyTransfer(long clientIdSender,
                              long clientIdRecipient,
                              BigDecimal money) {
        repository.moneyTransfer(clientIdSender, clientIdRecipient, money);
    }
}