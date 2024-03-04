package ru.sablin.app.bank.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.sablin.app.bank.email.EmailRepository;
import ru.sablin.app.bank.exception.*;
import ru.sablin.app.bank.phone.PhoneRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ClientRepositoryImpl implements ClientRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PhoneRepository phoneRepository;
    private final EmailRepository emailRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Transactional
    @Override
    public void create(Client client) {
        @Data
        @AllArgsConstructor
        class ParamsSql {
            long clientId;
            String values;
        }

        jdbcTemplate.update("""
                insert into Client (fio,
                                    birthday,
                                    login,
                                    password,
                                    start_balance,
                                    balance)
                values (?,?,?,?,?,?)
                """,
                client.getFio(),
                client.getBirthday(),
                client.getLogin(),
                client.getPassword(),
                client.getStartBalance(),
                client.getBalance());

        var clientId =
                jdbcTemplate.queryForObject(String.format("""
                select id
                from Client
                where login = '%s'
                """, client.getLogin()),
                    Integer.class);

        if (clientId != null) {
            List<ParamsSql> batchPhone = new ArrayList<>();
            for (String p : client.getPhone()) {
                batchPhone.add(new ParamsSql(clientId, p));
            }
            namedParameterJdbcTemplate.batchUpdate("insert into Phone values (:clientId, :values)",
                    SqlParameterSourceUtils.createBatch(batchPhone));

            List<ParamsSql> batchEmail = new ArrayList<>();
            for (String e : client.getEmail()) {
                batchEmail.add(new ParamsSql(clientId, e));
            }
            namedParameterJdbcTemplate.batchUpdate("insert into Email values(:clientId, :values)",
                    SqlParameterSourceUtils.createBatch(batchEmail));
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
        var clientId = 0;

        if (phone != null) {
            var listPhones = phoneRepository.findByPhone(phone);
            var clientIdByPhone = phoneRepository.findClientIdByPhone(phone);
            resultPhone.addAll(listPhones);
            if (email == null) {
                var listEmails = emailRepository.findByClientId(clientIdByPhone);
                resultEmail.addAll(listEmails);
            }
            clientId = clientIdByPhone;
        }

        if (email != null) {
            var listEmails = emailRepository.findByEmail(email);
            var clientIdByEmail = emailRepository.findClientIdByEmail(email);
            resultPhone.addAll(listEmails);
            if (phone == null) {
                var listPhones = phoneRepository.findByClientId(clientIdByEmail);
                resultEmail.addAll(listPhones);
            }
            clientId = clientIdByEmail;
        }

        if (email != null && phone != null) {
            var clientIdByEmail = emailRepository.findClientIdByEmail(email);
            var clientIdByPhone = phoneRepository.findClientIdByPhone(phone);

            // проверяем что client_id из таблицы телефоны и таблицы почта равны
            if (clientIdByPhone.equals(clientIdByEmail)) {
                // получаем все emails по clientIdByEmail из таблицы Email и добавляем в результирующий список
                var listEmails = emailRepository.findByClientId(clientIdByEmail);
                resultEmail.addAll(listEmails);
                // получаем все телефоны по clientIdByPhone из таблицы Phone и добавляем в результирующий список
                var listPhones = phoneRepository.findByClientId(clientIdByPhone);
                resultPhone.addAll(listPhones);
            }
            clientId = clientIdByEmail;
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
            for (Integer id : clientIdByParamsFioOrBirthday) {
                if (id.equals(clientId)) {
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
                for (ClientDto clientDto : listClientDtoByParams) {
                    // получаем по каждому клиенту все его email
                    var listEmail = emailRepository.findByClientId(clientDto.getId());
                    // получаем по каждому клиенту все его phone
                    var listPhone = phoneRepository.findByClientId(clientDto.getId());
                    // собираем список клиентов
                    resultListClient.add(
                            Client.builder()
                                    .id(clientDto.getId())
                                    .fio(clientDto.getFio())
                                    .birthday(clientDto.getBirthday())
                                    .phone(listPhone)
                                    .email(listEmail)
                                    .login(clientDto.getLogin())
                                    .password(clientDto.getPassword())
                                    .balance(clientDto.getBalance())
                                    .build());
                }
            }
        }
        return resultListClient;
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
            if (c.getBalance().doubleValue() < c.getStartBalance().doubleValue() * 2.07
                    && c.getBalance().doubleValue() + c.getBalance().doubleValue() * 0.05 <= c.getStartBalance().doubleValue() * 2.07) {
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