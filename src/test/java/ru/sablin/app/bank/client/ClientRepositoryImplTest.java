package ru.sablin.app.bank.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import ru.sablin.app.bank.client.exception.ClientNotFoundException;
import ru.sablin.app.bank.client.exception.MoneyInvalidException;
import ru.sablin.app.bank.client.exception.NotEnoughMoneyBalance;
import ru.sablin.app.bank.client.exception.TransferAndDebitFromOneAccountException;

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
    void moneyTransfer_isOk() {
        var clientSender = Client.builder()
                .id(1)
                .fio("Ivanov Ivan Ivanovich")
                .birthday(LocalDate.of(1999, 12, 25))
                .startBalance(BigDecimal.valueOf(100))
                .balance(BigDecimal.valueOf(100))
                .login("qwerty777")
                .password("postgres")
                .build();

        var clientRecipient = Client.builder()
                .id(2)
                .fio("Messi Leonel Semovich")
                .birthday(LocalDate.of(1989, 8, 7))
                .startBalance(BigDecimal.valueOf(100))
                .balance(BigDecimal.valueOf(100))
                .login("leoMessi10")
                .password("barca")
                .build();

        repository.moneyTransfer(clientSender.getId(), clientRecipient.getId(), BigDecimal.valueOf(20));

        var clientListRecipient = repository.findByParams(
                null,
                null,
                clientRecipient.getFio(),
                null);

        var clientListSender = repository.findByParams(
                null,
                null,
                clientSender.getFio(),
                null);

        // проверяем что деньги списались, и зачислились
        var expectedBalanceSender = clientListSender.get(0).getBalance();
        var expectedBalanceRecipient = clientListRecipient.get(0).getBalance();

        assertNotNull(expectedBalanceRecipient);
        assertNotNull(expectedBalanceSender);
        assertEquals(expectedBalanceSender, BigDecimal.valueOf(80));
        assertEquals(expectedBalanceRecipient, BigDecimal.valueOf(120));
    }

    @Test
    void moneyTransfer_IfMoneyIsNullOrNegative_returnException() {
        assertThrows(MoneyInvalidException.class, () ->
                repository.moneyTransfer(1, 2, null));
        assertThrows(MoneyInvalidException.class, () ->
                repository.moneyTransfer(1, 2, BigDecimal.valueOf(0)));
        assertThrows(MoneyInvalidException.class, () ->
                repository.moneyTransfer(1, 2, BigDecimal.valueOf(-1)));
    }

    @Test
    void moneyTransfer_IfClientNotFound__returnException() {
        // проверяем что метод выдаст исключение если отправитель не существует
        assertThrows(ClientNotFoundException.class, () ->
                repository.moneyTransfer(111, 2, BigDecimal.valueOf(20)));
        // проверяем что метод выдаст исключение если получатель не существует
        assertThrows(ClientNotFoundException.class, () ->
                repository.moneyTransfer(1, 200, BigDecimal.valueOf(30)));
    }

    @Test
    void moneyTransfer_IfNotEnoughMoneyBalance_returnException() {
        //проверяем что метод выдаст исключение если денег недостаточно для перевода
        assertThrows(NotEnoughMoneyBalance.class, () ->
                repository.moneyTransfer(1, 2, BigDecimal.valueOf(200)));
    }

    @Test
    void moneyTransfer_IfClientSenderAndClientRecipientEquals_returnException() {
        //проверяем что метод выдает исключение если счет списания и счет зачисления он же
        assertThrows(TransferAndDebitFromOneAccountException.class, () ->
                repository.moneyTransfer(1, 1, BigDecimal.valueOf(50)));
    }
}