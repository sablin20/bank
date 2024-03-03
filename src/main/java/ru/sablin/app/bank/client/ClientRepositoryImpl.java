package ru.sablin.app.bank.client;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.sablin.app.bank.client.exception.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ClientRepositoryImpl implements ClientRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    @Override
    public void create(Client client) {
        jdbcTemplate.update("""
                insert into Client (fio,
                                    birthday,
                                    login,
                                    password,
                                    balance)
                values (?,?,?,?,?)
                """,
                client.getFio(),
                client.getBirthday(),
                client.getLogin(),
                client.getPassword(),
                client.getBalance());

        var clientId =
                jdbcTemplate.queryForObject(String.format("""
                select id
                from Client
                where login = '%s'
                """, client.getLogin()),
                    Integer.class);

        if (clientId != null) {
            for (String p : client.getPhone()) {
                jdbcTemplate.update("insert into Phone values (?,?)", clientId, p);
            }

            for (String e : client.getEmail()) {
                jdbcTemplate.update("insert into Email values (?,?)", clientId, e);
            }
        }
    }

    @Override
    public List<Client> findByParams(LocalDate birthday,
                                     String phone,
                                     String fio,
                                     String email) {

        var resultListClient = new ArrayList<Client>();
        var resultEmail = new ArrayList<String>();
        var resultPhone = new ArrayList<String>();
        long clientId = 0;

        /**
         * phone
         */
        if (phone != null && email == null) {
            Integer clientIdByPhone;
            try {
                clientIdByPhone = jdbcTemplate.queryForObject("select client_id from Phone where phone = ?",
                        Integer.class, phone);
            } catch (EmptyResultDataAccessException e) {
                throw new PhoneException("Phone not found");
            }

            // получаем все телефоны по clientIdByPhone из таблицы Phone
            var listPhones = jdbcTemplate.queryForList("select phone from Phone where client_id = ?",
                    String.class, clientIdByPhone);

            // получаем все emails по clientIdByPhone из таблицы Email
            var listEmails = jdbcTemplate.queryForList("select email from Email where client_id = ?",
                    String.class, clientIdByPhone);

            // добавляем все почты в результирующий список для Client
            if (!listEmails.isEmpty()) {
                resultEmail.addAll(listEmails);
            }

            // добавляем все телефоны в результирующий список для Client
            if (!listPhones.isEmpty()) {
                resultPhone.addAll(listPhones);
            }

            // добавляем clientId по которому будем сравнивать, если придут другие параметры
            if (clientIdByPhone != null) {
                clientId = clientIdByPhone;
            }
            // по итогу у нас есть клиент ид, list email и list phone
        }

        /**
         * email
         */
        if (email != null && phone == null) {
            Integer clientIdByEmail;
            try {
                clientIdByEmail = jdbcTemplate.queryForObject("select client_id from Email where email = ?",
                        Integer.class, email);
            } catch (EmptyResultDataAccessException e) {
                throw new EmailException("Email not found in data base");
            }

            // получаем все emails по clientIdByEmail из таблицы Email
            var listEmails = jdbcTemplate.queryForList("select email from Email where client_id = ?",
                    String.class, clientIdByEmail);

            // получаем все телефоны по clientIdByEmail из таблицы Phone
            var listPhones = jdbcTemplate.queryForList("select phone from Phone where client_id = ?",
                    String.class, clientIdByEmail);

            // добавляем все почты в результирующий список для Client
            if (!listEmails.isEmpty()) {
                resultEmail.addAll(listEmails);
            }

            // добавляем все телефоны в результирующий список для Client
            if (!listPhones.isEmpty()) {
                resultPhone.addAll(listPhones);
            }

            // добавляем clientId по которому будем сравнивать, если придут другие параметры
            if (clientIdByEmail != null) {
                clientId = clientIdByEmail;
            }
            // по итогу у нас есть клиент ид, list email и list phone
        }

        /**
         * email and phone
         */
        if (email != null && phone != null) {
            Integer clientIdByEmail;
            try {
                // достаем client_id по почте
                clientIdByEmail = jdbcTemplate.queryForObject("select client_id from Email where email = ?",
                        Integer.class, email);
            } catch (EmptyResultDataAccessException e) {
                throw new EmailException("Email not found in data base");
            }

            Integer clientIdByPhone;
            try {
                // достаем client_id по телефону
                clientIdByPhone = jdbcTemplate.queryForObject("select client_id from Phone where phone = ?",
                        Integer.class, phone);
            } catch (EmptyResultDataAccessException e) {
                throw new PhoneException("Phone not found in data base");
            }

            // проверяем что client_id из таблицы телефоны и таблицы почта равны
            if ((clientIdByEmail != null && clientIdByPhone != null) && clientIdByPhone.equals(clientIdByEmail)) {

                // получаем все emails по clientIdByEmail из таблицы Email
                var listEmails = jdbcTemplate.queryForList("select email from Email where client_id = ?",
                        String.class, clientIdByEmail);

                // получаем все телефоны по clientIdByEmail из таблицы Phone
                var listPhones = jdbcTemplate.queryForList("select phone from Phone where client_id = ?",
                        String.class, clientIdByPhone);

                // добавляем все почты в результирующий список для Client
                if (!listEmails.isEmpty()) {
                    resultEmail.addAll(listEmails);
                }

                // добавляем все телефоны в результирующий список для Client
                if (!listPhones.isEmpty()) {
                    resultPhone.addAll(listPhones);
                }

                // добавляем clientId по которому будем сравнивать, если придут другие параметры
                clientId = clientIdByEmail;

                // по итогу у нас есть клиент ид, list email и list phone
            }
        }

        // формируем запрос по переданным параметрам
        var condition = "";

        if (fio != null) {
            condition += " WHERE ";
            condition += String.format("fio LIKE '%%%s%%'", fio);
        }

        if (birthday != null) {
            if (condition.isEmpty()) {
                condition += " WHERE ";
                condition += String.format("birthday > '%s'", birthday);
            } else {
                condition += " AND ";
                condition += String.format("birthday > '%s'", birthday);
            }
        }

        var sqlQuery = String.format("""
            SELECT id
            FROM Client
            %s
            """, condition);


        var clientIdByParamsFioOrBirthday =
                jdbcTemplate.queryForList(sqlQuery, Integer.class);

        // если у нас id равны значит возвращаем одного клиента
        if (!clientIdByParamsFioOrBirthday.isEmpty()) {
            for (Integer i : clientIdByParamsFioOrBirthday) {
                if (i.equals(clientId)) {
                    var client =
                            jdbcTemplate.queryForObject("""
                                select id, fio, birthday, login, password, balance 
                                from Client 
                                where id = ?
                                """,
                            (rs, rowNum) ->
                                Client.builder()
                                    .id(rs.getInt("id"))
                                    .fio(rs.getString("fio"))
                                    .birthday(rs.getObject("birthday", LocalDate.class))
                                    .email(resultEmail)
                                    .phone(resultPhone)
                                    .login(rs.getString("login"))
                                    .password(rs.getString("password"))
                                    .balance(rs.getBigDecimal("balance"))
                                    .build(), clientId);
                    resultListClient.add(client);
                    return resultListClient;
                }
            }
        }

        /**
         * если clientId == 0, значит не передали параметры phone и email,
         *  и это значит, что нужно получить из таблицы клиента все данные
         *  и по id клиента вытащить почты, телефоны и положить их в результирующие списки,
         *  и потом засунуть эти данные в список Dto и вернуть его
         */
        if (clientId == 0) {
            // получаем всех подходящих под фильтр клиентов
            var listClientDtoByParams = jdbcTemplate.query(String.format("select * from Client %s", condition),
                    (rs, rowNum) -> ClientDto.builder()
                            .id(rs.getInt("id"))
                            .fio(rs.getString("fio"))
                            .birthday(rs.getObject("birthday", LocalDate.class))
                            .balance(rs.getBigDecimal("balance"))
                            .login(rs.getString("login"))
                            .password(rs.getString("password"))
                            .build());

            // иначе выбросить исключение, что по таким параметрам клиент не найден
            if (listClientDtoByParams.isEmpty()) {
                throw new ClientNotFoundException("Client not found by current params");
            } else {
                for (ClientDto c : listClientDtoByParams) {
                    // получаем по каждому клиенту все его email
                    var listEmail = jdbcTemplate.queryForList("select email from Email where client_id = ?",
                            String.class, c.getId());

                    // получаем по каждому клиенту все его phone
                    var listPhone = jdbcTemplate.queryForList("select phone from Phone where client_id = ?",
                            String.class, c.getId());

                    // собираем список клиентов
                    resultListClient.add(
                            Client.builder()
                                    .id(c.getId())
                                    .fio(c.getFio())
                                    .birthday(c.getBirthday())
                                    .phone(listPhone)
                                    .email(listEmail)
                                    .login(c.getLogin())
                                    .password(c.getPassword())
                                    .balance(c.getBalance())
                                    .build());
                }
            }
        }
        return resultListClient;
    }

    @Override
    public void addEmail(long clientId, String email) {
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

        jdbcTemplate.update("insert into Email values (?,?)", clientId, email);
    }

    @Override
    public void addPhone(long clientId, String phone) {
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

        jdbcTemplate.update("insert into Phone values (?,?)", clientId, phone);
    }

    // нельзя удалить телефон если он последний у данного клиента,
    // должен остаться минимум 1
    @Override
    public void removePhone(String phone) {
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

        jdbcTemplate.update("delete from Phone where phone = ?", phone);
    }

    @Override
    public void removeEmail(String email) {
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

        jdbcTemplate.update("delete from Email where email = ?", email);
    }

    @Scheduled(cron = "*/60 * * * * *")
    @Override
    public void increaseInBalance() {
        var clientsList = jdbcTemplate.query("select id, start_balance, balance from Client",
                (rs, rowNum) -> Client.builder()
                                    .id(rs.getInt("id"))
                                    .startBalance(rs.getBigDecimal("start_balance"))
                                    .balance(rs.getBigDecimal("balance"))
                                    .build());

        for (Client c : clientsList) {
            if (c.getBalance().doubleValue() < c.getStartBalance().doubleValue() * 2.07) {
                jdbcTemplate.update("update Client set balance = ? where id = ?",
                        c.getBalance().doubleValue() + c.getBalance().doubleValue() * 0.05, c.getId());
            }
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public void moneyTransfer(long clientIdSender,
                              long clientIdRecipient,
                              BigDecimal money) {

        // проверяем что деньги указанны корректно
        if (money == null || money.intValue() < 1) {
            throw new MoneyInvalidException(String.format("Money = %s invalid", money));
        }

        // проверяем что клиент существует
        BigDecimal balanceSender;
        try {
            balanceSender = jdbcTemplate.queryForObject("select balance from Client where id = ?",
                    BigDecimal.class, clientIdSender);
        } catch (EmptyResultDataAccessException e) {
            throw new ClientNotFoundException(String.format("Client with id = %s is not found", clientIdSender));
        }

        // проверяем что клиент существует
        BigDecimal balanceRecipient;
        try {
            balanceRecipient = jdbcTemplate.queryForObject("select balance from Client where id = ?",
                    BigDecimal.class, clientIdRecipient);
        } catch (EmptyResultDataAccessException e) {
            throw new ClientNotFoundException(String.format("Client with id = %s is not found", clientIdRecipient));
        }

        //проверяем что денег достаточно для перевода
        if (money.intValue() > balanceSender.intValue()) {
            throw new NotEnoughMoneyBalance(String.format("There is not enough money in the account to transfer. Balance = %d",
                    balanceSender.intValue()));
        }

        // проверяем что счет списания и счет зачисления разные
        if (clientIdSender == clientIdRecipient) {
            throw new TransferAndDebitFromOneAccountException("Transfers and debits from the same account are prohibited");
        }

        // списываем деньги со счета отправителя
        jdbcTemplate.update("update Client set balance = ? where id = ?",
                balanceSender.intValue() - money.intValue(), clientIdSender);
        // зачисляем деньги на счет получателя
        jdbcTemplate.update("update Client set balance = ? where id = ?",
                balanceRecipient.intValue() + money.intValue(), clientIdRecipient);
    }
}