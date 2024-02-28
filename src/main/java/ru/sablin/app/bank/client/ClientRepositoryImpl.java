package ru.sablin.app.bank.client;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.sablin.app.bank.client.exception.ClientNotFoundException;
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
            var clientIdInDbPhone = jdbcTemplate.queryForObject("select client_id from Phone where phone = ?",
                    new BeanPropertyRowMapper<>(Integer.class), phone);

            // получаем все телефоны по clientIdInDbPhone из таблицы Phone
            var allPhoneInDbPhone = jdbcTemplate.query("select phone from Phone where client_id = ?",
                    new BeanPropertyRowMapper<>(String.class), clientIdInDbPhone);

            // получаем все emails по clientIdInDbPhone из таблицы Email
            var allEmailInDbEmail = jdbcTemplate.query("select email from Email where client_id = ?",
                    new BeanPropertyRowMapper<>(String.class), clientIdInDbPhone);

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
            var clientIdDbEmail = jdbcTemplate.queryForObject("select client_id from Email where email = ?",
                    new BeanPropertyRowMapper<>(Integer.class), email);

            // получаем все emails по clientIdInDbEmail из таблицы Email
            var allEmailInDbEmail = jdbcTemplate.query("select email from Email where client_id = ?",
                    new BeanPropertyRowMapper<>(String.class), clientIdDbEmail);

            // получаем все телефоны по clientIdInDbEmail из таблицы Phone
            var allPhoneInDbPhone = jdbcTemplate.query("select phone from Phone where client_id = ?",
                    new BeanPropertyRowMapper<>(String.class), clientIdDbEmail);

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
            // достаем client_id по почте
            var clientIdEmailDb = jdbcTemplate.queryForObject("select client_id from Email where email = ?",
                    new BeanPropertyRowMapper<>(Integer.class), email);

            // достаем client_id по телефону
            var clientIdInDbPhone = jdbcTemplate.queryForObject("select client_id from Phone where phone = ?",
                    new BeanPropertyRowMapper<>(Integer.class), phone);

            // проверяем что client_id из таблицы телефоны и таблицы почта равны
            if ((clientIdEmailDb != null && clientIdInDbPhone != null) && clientIdInDbPhone.equals(clientIdEmailDb)) {

                // получаем все emails по clientIdEmailDb из таблицы Email
                var allEmailInDbEmail = jdbcTemplate.query("select email from Email where client_id = ?",
                        new BeanPropertyRowMapper<>(String.class), clientIdEmailDb);

                // получаем все телефоны по clientIdInDbEmail из таблицы Phone
                var allPhoneInDbPhone = jdbcTemplate.query("select phone from Phone where client_id = ?",
                        new BeanPropertyRowMapper<>(String.class), clientIdInDbPhone);

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
            condition += getStrings(condition);
            condition += String.format("fio LIKE '%%%s%%'", fio);
        }

        if (birthday != null) {
            condition += getStrings(condition);
            condition += String.format("birthday > %s", birthday);
        }

        var sqlQuery = String.format("""
            SELECT id
            FROM Client
            %s
            """, condition);


        var clientIdInDbClientByParamsFioOrBirthday =
                jdbcTemplate.queryForObject(sqlQuery, new BeanPropertyRowMapper<>(Integer.class));

        // если у нас id равны значит возвращаем одного клиента
        if (clientIdInDbClientByParamsFioOrBirthday != null && clientIdInDbClientByParamsFioOrBirthday.equals(clientId)) {
           var clientDTO = jdbcTemplate.queryForObject("select id, fio, birthday from Client where id = ?", (rs, rowNum) ->
                    ClientDto.builder()
                            .id(rs.getInt("id"))
                            .fio(rs.getString("fio"))
                            .birthday(rs.getObject("birthday", LocalDate.class))
                            .email(resultEmail)
                            .phone(resultPhone)
                            .build(), clientId);
           resultListClientDto.add(clientDTO);
           return resultListClientDto;

        // если clientId == 0, значит не передали параметры phone и email,
        // и это значит, что нужно получить из таблицы клиента все данные
        // и по id клиента вытащить почты, телефоны и положить их в результирующие списки,
        // и потом засунуть эти данные в список Dto и вернуть его
        } else if (clientId == 0) {
            // получаем всех подходящих под фильтр клиентов
            var clientDtoListByParams = jdbcTemplate.query(String.format("select * from Client %s", condition),
                    new BeanPropertyRowMapper<>(ClientDtoDbTable.class));

            for (ClientDtoDbTable c : clientDtoListByParams) {
                // получаем по каждому клиенту все его email
                var listEmail = jdbcTemplate.query("select email from Email where client_id = ?",
                        new BeanPropertyRowMapper<>(String.class), c.getId());

                // получаем по каждому клиенту все его phone
                var listPhone = jdbcTemplate.query("select phone from Phone where client_id = ?",
                        new BeanPropertyRowMapper<>(String.class), c.getId());

                // собираем список клиентов
                resultListClientDto.add(
                        ClientDto.builder()
                            .id(c.getId())
                            .fio(c.getFio())
                            .birthday(c.getBirthday())
                            .phone(listPhone)
                            .email(listEmail)
                            .build());
            }
        // иначе выбросить исключение, что по таким параметрам клиент не найден
        } else {
            throw new ClientNotFoundException("Client not found by current params");
        }
        return resultListClientDto;
    }

    private String getStrings(String condition) {
        if (!condition.isEmpty()) {
            condition += " AND ";
        } else {
            condition += " WHERE ";
        }
        return condition;
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