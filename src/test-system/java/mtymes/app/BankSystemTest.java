package mtymes.app;

import com.mongodb.client.MongoDatabase;
import javafixes.math.Decimal;
import mtymes.account.app.Bank;
import mtymes.account.config.SystemProperties;
import mtymes.account.domain.account.AccountId;
import mtymes.api.BankApi;
import mtymes.api.ResponseWrapper;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import static de.flapdoodle.embed.process.runtime.Network.getFreeServerPort;
import static javafixes.math.Decimal.ZERO;
import static javafixes.math.Decimal.d;
import static mtymes.common.json.JsonBuilder.emptyJson;
import static mtymes.common.json.JsonBuilder.jsonBuilder;
import static mtymes.test.Random.*;

public class BankSystemTest {

    private static EmbeddedDB db;
    private static Bank bank;
    private static BankApi api;

    @BeforeClass
    public static void initDbAndApp() throws IOException {
        db = MongoManager.getEmbeddedDB();
        MongoDatabase database = db.getDatabase();

        SystemProperties properties = new SystemProperties(
                getFreeServerPort(),
                "localhost",
                db.getPort(),
                db.getDbName(),
                10,
                Duration.ofMillis(0)
        );
        bank = new Bank(properties).start();

        api = new BankApi("localhost", bank.getPort());
    }

    @AfterClass
    public static void releaseDB() {
        MongoManager.release(db);
        bank.shutdown();
    }

    @Test
    public void shouldFailToLoadNonExistingAccount() {
        AccountId nonExistingAccountId = randomAccountId();

        // When & Then
        api.loadAccount(nonExistingAccountId)
                .shouldHaveStatus(400)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "Account '" + nonExistingAccountId + "' not found")
                        .build());
    }

    @Test
    public void shouldBeAbleToCreateAccount() {
        // When & Then
        ResponseWrapper response = api.createAccount()
                .shouldHaveStatus(201);

        AccountId accountId = response.accountId();

        response.shouldHaveBody(jsonBuilder()
                .with("accountId", accountId)
                .with("balance", ZERO)
                .with("version", 1)
                .build());

        api.loadAccount(accountId)
                .shouldHaveStatus(200)
                .shouldHaveBody(jsonBuilder()
                        .with("accountId", accountId)
                        .with("balance", ZERO)
                        .with("version", 1)
                        .build());
    }

    @Test
    public void shouldBeAbleToDepositMoney() {
        AccountId accountId = api.createAccount().accountId();
        Decimal amount = randomPositiveAmount();

        // When & Then
        api.depositMoney(accountId, amount)
                .shouldHaveStatus(200)
                .shouldHaveBody(emptyJson());

        api.loadAccount(accountId).shouldHaveBody(jsonBuilder()
                .with("accountId", accountId)
                .with("balance", amount)
                .with("version", 2)
                .build());
    }

    @Test
    public void shouldFailToDepositZeroAmount() {
        AccountId accountId = api.createAccount().accountId();
        Decimal amount = ZERO;

        // When & Then
        api.depositMoney(accountId, amount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "amount must be a positive value")
                        .build());

        api.loadAccount(accountId).shouldHaveBody(jsonBuilder()
                .with("accountId", accountId)
                .with("balance", ZERO)
                .with("version", 1)
                .build());
    }

    @Test
    public void shouldFailToDepositNegativeAmount() {
        AccountId accountId = api.createAccount().accountId();
        Decimal amount = randomNegativeAmount();

        // When & Then
        api.depositMoney(accountId, amount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "amount must be a positive value")
                        .build());

        api.loadAccount(accountId).shouldHaveBody(jsonBuilder()
                .with("accountId", accountId)
                .with("balance", ZERO)
                .with("version", 1)
                .build());
    }

    @Test
    public void shouldFailToDepositMoneyToNonExistingAccount() {
        AccountId nonExistingAccountId = randomAccountId();
        Decimal amount = randomPositiveAmount();

        // When & Then
        api.depositMoney(nonExistingAccountId, amount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "Account '" + nonExistingAccountId + "' does not exist")
                        .build());
    }

    @Test
    public void shouldBeAbleToWithdrawMoney() {
        AccountId accountId = api.createAccount().accountId();
        Decimal initialDeposit = randomAmountBetween(d("0.02"), d("100_000.00"));
        api.depositMoney(accountId, initialDeposit);

        // When & Then
        Decimal amount = randomAmountBetween(d("0.01"), initialDeposit.minus(d("0.01")));
        api.withdrawMoney(accountId, amount)
                .shouldHaveStatus(200)
                .shouldHaveBody(emptyJson());

        api.loadAccount(accountId).shouldHaveBody(jsonBuilder()
                .with("accountId", accountId)
                .with("balance", initialDeposit.minus(amount))
                .with("version", 3)
                .build());
    }

    @Test
    public void shouldBeAbleToWithdrawMoneyAndGetBalanceToZero() {
        AccountId accountId = api.createAccount().accountId();
        Decimal initialDeposit = randomAmountBetween(d("0.02"), d("100_000.00"));
        api.depositMoney(accountId, initialDeposit);

        // When & Then
        Decimal amount = initialDeposit;
        api.withdrawMoney(accountId, amount)
                .shouldHaveStatus(200)
                .shouldHaveBody(emptyJson());

        api.loadAccount(accountId).shouldHaveBody(jsonBuilder()
                .with("accountId", accountId)
                .with("balance", ZERO)
                .with("version", 3)
                .build());
    }

    @Test
    public void shouldFailToWithdrawMoneyIfAccountHasInsufficientFunds() {
        AccountId accountId = api.createAccount().accountId();
        Decimal initialDeposit = randomPositiveAmount();
        api.depositMoney(accountId, initialDeposit);

        // When & Then
        Decimal amount = initialDeposit.plus(d("0.01"));
        api.withdrawMoney(accountId, amount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "Insufficient funds on account '" + accountId + "'")
                        .build());

        api.loadAccount(accountId).shouldHaveBody(jsonBuilder()
                .with("accountId", accountId)
                .with("balance", initialDeposit)
                .with("version", 2)
                .build());
    }

    @Test
    public void shouldFailToWithdrawZeroAmount() {
        AccountId accountId = api.createAccount().accountId();
        Decimal initialDeposit = randomPositiveAmount();
        api.depositMoney(accountId, initialDeposit);

        // When & Then
        Decimal amount = ZERO;
        api.withdrawMoney(accountId, amount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "amount must be a positive value")
                        .build());

        api.loadAccount(accountId).shouldHaveBody(jsonBuilder()
                .with("accountId", accountId)
                .with("balance", initialDeposit)
                .with("version", 2)
                .build());
    }

    @Test
    public void shouldFailToWithdrawNegativeAmount() {
        AccountId accountId = api.createAccount().accountId();
        Decimal initialDeposit = randomPositiveAmount();
        api.depositMoney(accountId, initialDeposit);

        // When & Then
        Decimal amount = randomNegativeAmount();
        api.withdrawMoney(accountId, amount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "amount must be a positive value")
                        .build());

        api.loadAccount(accountId).shouldHaveBody(jsonBuilder()
                .with("accountId", accountId)
                .with("balance", initialDeposit)
                .with("version", 2)
                .build());
    }

    @Test
    public void shouldFailToWithdrawMoneyFromNonExistingAccount() {
        AccountId nonExistingAccountId = randomAccountId();
        Decimal amount = randomPositiveAmount();

        // When & Then
        api.withdrawMoney(nonExistingAccountId, amount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "Account '" + nonExistingAccountId + "' does not exist")
                        .build());
    }

    @Test
    public void shouldBeAbleToTransferMoney() {
        // todo: implement
    }

    @Test
    public void shouldBeAbleToTransferMoneyAndGetBalanceToZero() {
        // todo: implement
    }

    @Test
    public void shouldFailToTransferMoneyFromNonExistingAccount() {
        // todo: implement
    }

    @Test
    public void shouldFailToTransferMoneyToNonExistingAccount() {
        // todo: implement
    }

    @Test
    public void shouldFailToTransferMoneyIfFromAccountHasInsufficientFunds() {
        // todo: implement
    }


//    @Deprecated // todo: remove - make part of other system tests
//    @Test
//    public void shouldLoadAccount() throws Exception {
//        SystemProperties properties = new SystemProperties(
//                getFreeServerPort(),
//                "localhost",
//                db.getPort(),
//                db.getDbName(),
//                10,
//                Duration.ofMillis(0)
//        );
//        Bank bank = new Bank(properties).start();
//
//        BankApi api = new BankApi("localhost", bank.getPort());
//
//        Response account = api.createAccount();
//        System.out.println(account);
////        Response response = api.loadAccount(randomAccountId().toString());
////        System.out.println(response);
//    }
}
