package ru.sablin.app.bank.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql({"/schema-drop.sql", "/schema-test.sql"})
@Sql(scripts = "/data-test.sql")
class ClientRepositoryImplTest {

    @Autowired
    ClientRepositoryImpl repository;

    @Test
    void moneyTransfer() {
        var client1 = Client.builder()
                .id(1)
                .fio("Ivanov Ivan Ivanovich")
                .birthday(LocalDate.of(1999, 12, 25))
                .startBalance(BigDecimal.valueOf(100))
                .balance(BigDecimal.valueOf(100))
                .login("qwerty777")
                .password("postgres")
                .build();

        var client2 = Client.builder()
                .id(2)
                .fio("Messi Leonel Semovich")
                .birthday(LocalDate.of(1989, 8, 7))
                .startBalance(BigDecimal.valueOf(100))
                .balance(BigDecimal.valueOf(100))
                .login("leoMessi10")
                .password("barca")
                .build();

        repository.moneyTransfer(client1.getId(), client2.getId(), BigDecimal.valueOf(20));

        var clientList = repository.findByParam(
                LocalDate.of(1989, 8, 7),
                "89996664411",
                "Messi Leonel Semovich",
                "sadadada@mail.ru");

        var expectedBalance = clientList.get(0).getBalance();

        assertNotNull(expectedBalance);
        assertEquals(expectedBalance, BigDecimal.valueOf(120));
    }
}