package ru.sablin.app.bank.client;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.sablin.app.bank.client.exception.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ClientRepositoryImpl implements ClientRepository {

    private final JdbcTemplate jdbcTemplate;
    NamedParameterJdbcTemplate namedParamJdbcTemplate;

    @Transactional
    @Override
    public void create(Client client) {
        jdbcTemplate.update("insert into Client (fio, birthday, login, password, balance) values (?,?,?,?,?)",
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
    public List<ClientDto> findByParam(LocalDate birthday,
                                       String phone,
                                       String fio,
                                       String email) {

        var resultListClientDto = new ArrayList<ClientDto>();
        var resultEmail = new ArrayList<String>();
        var resultPhone = new ArrayList<String>();
        var clientId = 0;

        /**
         * phone
         */
        if (phone != null && email == null) {
            Integer clientIdInDbPhone;
            try {
                clientIdInDbPhone = jdbcTemplate.queryForObject("select client_id from Phone where phone = ?",
                        Integer.class, phone);
            } catch (EmptyResultDataAccessException e) {
                throw new PhoneException("Phone not found");
            }

            // получаем все телефоны по clientIdInDbPhone из таблицы Phone
            var allPhoneInDbPhone = jdbcTemplate.queryForList("select phone from Phone where client_id = ?",
                    String.class, clientIdInDbPhone);

            // получаем все emails по clientIdInDbPhone из таблицы Email
            var allEmailInDbEmail = jdbcTemplate.queryForList("select email from Email where client_id = ?",
                    String.class, clientIdInDbPhone);

            // добавляем все почты в результирующий список для ClientDto
            if (!allEmailInDbEmail.isEmpty()) {
                resultEmail.addAll(allEmailInDbEmail);
            }

            // добавляем все телефоны в результирующий список для ClientDto
            if (!allPhoneInDbPhone.isEmpty()) {
                resultPhone.addAll(allPhoneInDbPhone);
            }

            // добавляем clientId по которому будем сравнивать, если придут другие параметры
            if (clientIdInDbPhone != null) {
                clientId += clientIdInDbPhone;
            }
            // по итогу у нас есть клиент ид, list email и list phone
        }

        /**
         * email
         */
        if (email != null && phone == null) {

            Integer clientIdDbEmail;
            try {
                clientIdDbEmail = jdbcTemplate.queryForObject("select client_id from Email where email = ?",
                        Integer.class, email);
            } catch (EmptyResultDataAccessException e) {
                throw new EmailException("Email not found in data base");
            }

            // получаем все emails по clientIdInDbEmail из таблицы Email
            var allEmailInDbEmail = jdbcTemplate.queryForList("select email from Email where client_id = ?",
                    String.class, clientIdDbEmail);

            // получаем все телефоны по clientIdInDbEmail из таблицы Phone
            var allPhoneInDbPhone = jdbcTemplate.queryForList("select phone from Phone where client_id = ?",
                    String.class, clientIdDbEmail);

            // добавляем все почты в результирующий список для ClientDto
            if (!allEmailInDbEmail.isEmpty()) {
                resultEmail.addAll(allEmailInDbEmail);
            }

            // добавляем все телефоны в результирующий список для ClientDto
            if (!allPhoneInDbPhone.isEmpty()) {
                resultPhone.addAll(allPhoneInDbPhone);
            }

            // добавляем clientId по которому будем сравнивать, если придут другие параметры
            if (clientIdDbEmail != null) {
                clientId += clientIdDbEmail;
            }
            // по итогу у нас есть клиент ид, list email и list phone
        }

        /**
         * email and phone
         */
        if (email != null && phone != null) {

            Integer clientIdEmailDb;
            try {
                // достаем client_id по почте
                clientIdEmailDb = jdbcTemplate.queryForObject("select client_id from Email where email = ?",
                        Integer.class, email);
            } catch (EmptyResultDataAccessException e) {
                throw new EmailException("Email not found in data base");
            }

            Integer clientIdInDbPhone;
            try {
                // достаем client_id по телефону
                clientIdInDbPhone = jdbcTemplate.queryForObject("select client_id from Phone where phone = ?",
                        Integer.class, phone);
            } catch (EmptyResultDataAccessException e) {
                throw new PhoneException("Phone not found in data base");
            }

            // проверяем что client_id из таблицы телефоны и таблицы почта равны
            if ((clientIdEmailDb != null && clientIdInDbPhone != null) && clientIdInDbPhone.equals(clientIdEmailDb)) {

                // получаем все emails по clientIdEmailDb из таблицы Email
                var allEmailInDbEmail = jdbcTemplate.queryForList("select email from Email where client_id = ?",
                        String.class, clientIdEmailDb);

                // получаем все телефоны по clientIdInDbEmail из таблицы Phone
                var allPhoneInDbPhone = jdbcTemplate.queryForList("select phone from Phone where client_id = ?",
                        String.class, clientIdInDbPhone);

                // добавляем все почты в результирующий список для ClientDto
                if (!allEmailInDbEmail.isEmpty()) {
                    resultEmail.addAll(allEmailInDbEmail);
                }

                // добавляем все телефоны в результирующий список для ClientDto
                if (!allPhoneInDbPhone.isEmpty()) {
                    resultPhone.addAll(allPhoneInDbPhone);
                }

                // добавляем clientId по которому будем сравнивать, если придут другие параметры
                clientId = clientIdEmailDb;

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


        var clientIdInDbClientByParamsFioOrBirthday =
                jdbcTemplate.queryForList(sqlQuery, Integer.class);

        // если у нас id равны значит возвращаем одного клиента
        if (!clientIdInDbClientByParamsFioOrBirthday.isEmpty()) {
            for (Integer i : clientIdInDbClientByParamsFioOrBirthday) {
                if (i.equals(clientId)) {
                    var clientDTO =
                            jdbcTemplate.queryForObject("""
                                select id, fio, birthday, login, password, balance 
                                from Client 
                                where id = ?
                                """,
                            (rs, rowNum) ->
                                ClientDto.builder()
                                    .id(rs.getInt("id"))
                                    .fio(rs.getString("fio"))
                                    .birthday(rs.getObject("birthday", LocalDate.class))
                                    .email(resultEmail)
                                    .phone(resultPhone)
                                    .login(rs.getString("login"))
                                    .password(rs.getString("password"))
                                    .balance(rs.getBigDecimal("balance"))
                                    .build(), clientId);
                    resultListClientDto.add(clientDTO);
                    return resultListClientDto;
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
            var clientDtoListByParams = jdbcTemplate.query(String.format("select * from Client %s", condition),
                    (rs, rowNum) -> ClientDtoDbTable.builder()
                            .id(rs.getInt("id"))
                            .fio(rs.getString("fio"))
                            .birthday(rs.getObject("birthday", LocalDate.class))
                            .balance(rs.getBigDecimal("balance"))
                            .login(rs.getString("login"))
                            .password(rs.getString("password"))
                            .build());

            // иначе выбросить исключение, что по таким параметрам клиент не найден
            if (clientDtoListByParams.isEmpty()) {
                throw new ClientNotFoundException("Client not found by current params");
            } else {
                for (ClientDtoDbTable c : clientDtoListByParams) {
                    // получаем по каждому клиенту все его email
                    var listEmail = jdbcTemplate.queryForList("select email from Email where client_id = ?",
                            String.class, c.getId());

                    // получаем по каждому клиенту все его phone
                    var listPhone = jdbcTemplate.queryForList("select phone from Phone where client_id = ?",
                            String.class, c.getId());

                    // собираем список клиентов
                    resultListClientDto.add(
                            ClientDto.builder()
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
        return resultListClientDto;
    }

    @Override
    public void addEmail(Integer clientId, String email) {
        // проверяем что есть клиент с таким id
        try {
            var idClient = jdbcTemplate.queryForObject("select id from Client where id = ?",
                    Integer.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            throw new ClientNotFoundException(String.format("Not found client with current id = %s", clientId));
        }

        // проверяем что почта свободна
        var listEmail = jdbcTemplate.queryForList("select email from Email", String.class);
        if (listEmail.contains(email)) {
            throw new EmailBusyException(String.format("Email = %s, busy", email));
        }

        jdbcTemplate.update("insert into Email values (?,?)", clientId, email);
    }

    @Override
    public void addPhone(Integer clientId, String phone) {
        // проверяем что есть клиент с таким id
        try {
            var idClient = jdbcTemplate.queryForObject("select id from Client where id = ?",
                    Integer.class, clientId);
        } catch (EmptyResultDataAccessException e) {
            throw new ClientNotFoundException(String.format("Not found client with current id = %s", clientId));
        }

        // проверяем что телефон свободен
        var listPhone = jdbcTemplate.queryForList("select phone from Phone", String.class);
        if (listPhone.contains(phone)) {
            throw new PhoneBusyException(String.format("Phone = %s, busy", phone));
        }

        jdbcTemplate.update("insert into Phone values (?,?)", clientId, phone);
    }

    // нельзя удалить телефон если он последний у данного клиента,
    // должен остаться минимум 1
    @Override
    public void removePhone(String phone) {
        // получаем список телефонов
        var listPhone = jdbcTemplate.queryForList("select phone from Phone", String.class);

        // проверяем что такой телефон существует
        if (listPhone.isEmpty() || !listPhone.contains(phone)) {
            throw new PhoneException(String.format("Phone = %s not found", phone));
        }

        var list = jdbcTemplate.queryForList("""
                select phone
                from Phone
                where client_id =
                    (select client_id
                    from Phone
                    where phone = ?)
                """,
                String.class, phone);

        // проверяем что телефон не последний
        if (list.size() <= 1) {
            throw new PhoneLastException(String.format("This phone = %s, last", phone));
        }

        jdbcTemplate.update("delete from Phone where phone = ?", phone);
    }

    @Override
    public void removeEmail(String email) {
        // получаем список email
        var listEmail = jdbcTemplate.queryForList("select email from Email", String.class);

        // проверяем что такой email существует
        if (listEmail.isEmpty() || !listEmail.contains(email)) {
            throw new EmailException(String.format("Email = %s not found", email));
        }

        var list = jdbcTemplate.queryForList("""
                select email
                from Email
                where client_id =
                    (select client_id
                    from Email
                    where email = ?)
                """,
                String.class, email);

        // проверяем что email не последний
        if (list.size() <= 1) {
            throw new EmailLastException(String.format("This email = %s, last", email));
        }

        jdbcTemplate.update("delete from Email where email = ?", email);
    }
}