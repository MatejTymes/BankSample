package mtymes.app;

import com.fasterxml.jackson.databind.node.ObjectNode;
import javafixes.math.Decimal;
import mtymes.account.app.Bank;
import mtymes.account.config.SystemProperties;
import mtymes.account.domain.account.AccountId;
import mtymes.api.BankApi;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import static de.flapdoodle.embed.process.runtime.Network.getFreeServerPort;
import static javafixes.math.Decimal.d;
import static mtymes.common.json.JsonBuilder.jsonBuilder;
import static mtymes.test.Random.randomAmountBetween;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class TwoNodesSystemTest {

    private static EmbeddedDB db;
    private static Bank appNode1;
    private static Bank appNode2;
    private static BankApi node1Api;
    private static BankApi node2Api;

    @BeforeClass
    public static void initDbAndApp() throws IOException {
        db = MongoManager.getEmbeddedDB();

        SystemProperties properties1 = new SystemProperties(
                getFreeServerPort(),
                "localhost",
                db.getPort(),
                db.getDbName(),
                10,
                Duration.ofMillis(0)
        );
        appNode1 = new Bank(properties1).start();
        SystemProperties properties2 = new SystemProperties(
                getFreeServerPort(),
                "localhost",
                db.getPort(),
                db.getDbName(),
                10,
                Duration.ofMillis(0)
        );
        appNode2 = new Bank(properties2).start();

        node1Api = new BankApi("localhost", appNode1.getPort());
        node2Api = new BankApi("localhost", appNode2.getPort());

        assertThat(appNode1.getPort(), not(equalTo(appNode2.getPort())));
    }

    @AfterClass
    public static void releaseDB() {
        MongoManager.release(db);
        appNode1.shutdown();
        appNode2.shutdown();
    }

    @Test
    public void shouldBeAbleToAccessTheSameDataFromDifferentNodes() {
        // When
        AccountId accountId = node1Api.createAccount().accountId();

        Decimal initialDeposit = randomAmountBetween(d("10_000"), d("100_000"));
        node1Api.depositMoney(accountId, initialDeposit);

        Decimal firstWithdrawal = randomAmountBetween(d("250"), d("500"));
        node2Api.withdrawMoney(accountId, firstWithdrawal);

        Decimal secondWithdrawal = randomAmountBetween(d("10"), d("45"));
        node1Api.withdrawMoney(accountId, secondWithdrawal);

        // Then
        ObjectNode expectedAccountStatus = jsonBuilder()
                .with("accountId", accountId)
                .with("balance", initialDeposit.minus(firstWithdrawal).minus(secondWithdrawal))
                .with("version", 4)
                .build();
        node1Api.loadAccount(accountId)
                .shouldHaveStatus(200)
                .shouldHaveBody(expectedAccountStatus);
        node2Api.loadAccount(accountId)
                .shouldHaveStatus(200)
                .shouldHaveBody(expectedAccountStatus);
    }
}
