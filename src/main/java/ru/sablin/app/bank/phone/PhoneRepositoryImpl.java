package ru.sablin.app.bank.phone;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PhoneRepositoryImpl implements PhoneRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void create(long clientId, String phone) {
        jdbcTemplate.update("insert into Phone values (?,?)", clientId, phone);
    }

    @Override
    public void delete(String phone) {
        jdbcTemplate.update("delete from Phone where phone = ?", phone);
    }
}
