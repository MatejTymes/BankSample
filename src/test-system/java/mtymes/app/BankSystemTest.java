package mtymes.app;

import com.mongodb.client.MongoDatabase;
import javafixes.math.Decimal;
import mtymes.account.app.Bank;
import mtymes.account.config.SystemProperties;
import mtymes.account.domain.QueuedWorkStats;
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
        AccountId fromAccountId = api.createAccount().accountId();
        AccountId toAccountId = api.createAccount().accountId();

        Decimal initialDeposit = randomAmountBetween(d("10.00"), d("99_999.99"));
        api.depositMoney(fromAccountId, initialDeposit);

        Decimal transferAmount = randomAmountBetween(d("0.01"), initialDeposit.minus(d("0.01")));

        // When & Then
        api.transferMoney(fromAccountId, toAccountId, transferAmount)
                .shouldHaveStatus(200)
                .shouldHaveBody(emptyJson());

        api.loadAccount(fromAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", fromAccountId)
                .with("balance", initialDeposit.minus(transferAmount))
                .with("version", 3)
                .build());
        waitForQueuedWorkToFinish();
        api.loadAccount(toAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", toAccountId)
                .with("balance", transferAmount)
                .with("version", 2)
                .build());
    }

    @Test
    public void shouldBeAbleToTransferMoneyAndGetBalanceToZero() {
        AccountId fromAccountId = api.createAccount().accountId();
        AccountId toAccountId = api.createAccount().accountId();

        Decimal initialDeposit = randomAmountBetween(d("10.00"), d("99_999.99"));
        api.depositMoney(fromAccountId, initialDeposit);

        Decimal transferAmount = initialDeposit;

        // When & Then
        api.transferMoney(fromAccountId, toAccountId, transferAmount)
                .shouldHaveStatus(200)
                .shouldHaveBody(emptyJson());

        api.loadAccount(fromAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", fromAccountId)
                .with("balance", ZERO)
                .with("version", 3)
                .build());
        waitForQueuedWorkToFinish();
        api.loadAccount(toAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", toAccountId)
                .with("balance", transferAmount)
                .with("version", 2)
                .build());
    }

    @Test
    public void shouldFailToTransferMoneyIfFromAccountHasInsufficientFunds() {
        AccountId fromAccountId = api.createAccount().accountId();
        AccountId toAccountId = api.createAccount().accountId();

        Decimal initialDeposit = randomAmountBetween(d("10.00"), d("99_999.99"));
        api.depositMoney(fromAccountId, initialDeposit);

        Decimal transferAmount = initialDeposit.plus(d("0.01"));

        // When & Then
        api.transferMoney(fromAccountId, toAccountId, transferAmount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "Insufficient funds on account '" + fromAccountId + "'")
                        .build());

        api.loadAccount(fromAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", fromAccountId)
                .with("balance", initialDeposit)
                .with("version", 2)
                .build());
        waitForQueuedWorkToFinish();
        api.loadAccount(toAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", toAccountId)
                .with("balance", ZERO)
                .with("version", 1)
                .build());
    }

    @Test
    public void shouldFailToTransferZeroAmount() {
        AccountId fromAccountId = api.createAccount().accountId();
        AccountId toAccountId = api.createAccount().accountId();

        Decimal initialDeposit = randomAmountBetween(d("10.00"), d("99_999.99"));
        api.depositMoney(fromAccountId, initialDeposit);

        Decimal transferAmount = ZERO;

        // When & Then
        api.transferMoney(fromAccountId, toAccountId, transferAmount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "amount must be a positive value")
                        .build());

        api.loadAccount(fromAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", fromAccountId)
                .with("balance", initialDeposit)
                .with("version", 2)
                .build());
        waitForQueuedWorkToFinish();
        api.loadAccount(toAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", toAccountId)
                .with("balance", ZERO)
                .with("version", 1)
                .build());
    }

    @Test
    public void shouldFailToTransferNegativeAmount() {
        AccountId fromAccountId = api.createAccount().accountId();
        AccountId toAccountId = api.createAccount().accountId();

        Decimal initialDeposit = randomAmountBetween(d("10.00"), d("99_999.99"));
        api.depositMoney(fromAccountId, initialDeposit);

        Decimal transferAmount = randomNegativeAmount();

        // When & Then
        api.transferMoney(fromAccountId, toAccountId, transferAmount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "amount must be a positive value")
                        .build());

        api.loadAccount(fromAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", fromAccountId)
                .with("balance", initialDeposit)
                .with("version", 2)
                .build());
        waitForQueuedWorkToFinish();
        api.loadAccount(toAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", toAccountId)
                .with("balance", ZERO)
                .with("version", 1)
                .build());
    }

    @Test
    public void shouldFailToTransferMoneyFromNonExistingAccount() {
        AccountId fromAccountId = randomAccountId();
        AccountId toAccountId = api.createAccount().accountId();

        Decimal transferAmount = randomPositiveAmount();

        // When & Then
        api.transferMoney(fromAccountId, toAccountId, transferAmount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "From Account '" + fromAccountId + "' does not exist")
                        .build());

        api.loadAccount(fromAccountId)
                .shouldHaveStatus(400)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "Account '" + fromAccountId + "' not found")
                        .build());
        waitForQueuedWorkToFinish();
        api.loadAccount(toAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", toAccountId)
                .with("balance", ZERO)
                .with("version", 1)
                .build());
    }

    @Test
    public void shouldFailToTransferMoneyToNonExistingAccount() {
        AccountId fromAccountId = api.createAccount().accountId();
        AccountId toAccountId = randomAccountId();

        Decimal initialDeposit = randomAmountBetween(d("10.00"), d("99_999.99"));
        api.depositMoney(fromAccountId, initialDeposit);

        Decimal transferAmount = randomAmountBetween(d("0.01"), initialDeposit.minus(d("0.01")));

        // When & Then
        api.transferMoney(fromAccountId, toAccountId, transferAmount)
                .shouldHaveStatus(500)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "To Account '" + toAccountId + "' does not exist")
                        .build());

        api.loadAccount(fromAccountId).shouldHaveBody(jsonBuilder()
                .with("accountId", fromAccountId)
                .with("balance", initialDeposit)
                .with("version", 2)
                .build());
        waitForQueuedWorkToFinish();
        api.loadAccount(toAccountId)
                .shouldHaveStatus(400)
                .shouldHaveBody(jsonBuilder()
                        .with("message", "Account '" + toAccountId + "' not found")
                        .build());
    }

    private void waitForQueuedWorkToFinish(Duration duration) {
        long startTime = System.currentTimeMillis();

        boolean retry;
        do {
            long currentTime = System.currentTimeMillis();
            QueuedWorkStats stats = api.queuedWorkStats().bodyAs(QueuedWorkStats.class);
            retry = stats.queuedCount > 0 || stats.inProgressCount > 0;
            if (retry && currentTime - startTime > duration.toMillis()) {
                throw new IllegalStateException("queued work has not finished in " + duration);
            }

            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        } while (retry);
    }

    private void waitForQueuedWorkToFinish() {
        waitForQueuedWorkToFinish(Duration.ofSeconds(2));
    }
}
