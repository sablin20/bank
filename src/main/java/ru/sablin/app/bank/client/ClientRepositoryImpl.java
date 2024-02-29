package ru.sablin.app.bank.client;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.sablin.app.bank.client.exception.ClientNotFoundException;
import ru.sablin.app.bank.client.exception.EmailException;
import ru.sablin.app.bank.client.exception.PhoneException;

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

        for (String p : client.getPhone()) {
            jdbcTemplate.update("INSERT INTO Phone VALUES (?,?)", client.getId(), p);
        }

        for (String e : client.getEmail()) {
            jdbcTemplate.update("INSERT INTO Email VALUES (?,?)", client.getId(), e);
        }

        jdbcTemplate.update("INSERT INTO Client VALUES (?,?,?,?,?,?)",
                client.getId(),
                client.getFio(),
                client.getBirthday(),
                client.getLogin(),
                client.getPassword(),
                client.getBalance());

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
                throw new PhoneException("Phone not found in data base");
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

        jdbcTemplate.update("insert into Email values (?,?)", clientId, email);
    }

    @Override
    public void addPhone(Integer clientId, String phone) {

    }

    @Override
    public void removePhone(Integer clientId, String phone) {

    }

    @Override
    public void removeEmail(Integer clientId, String email) {

    }
}