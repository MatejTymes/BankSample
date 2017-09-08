package mtymes.app;

import com.mongodb.client.MongoDatabase;
import mtymes.account.app.Bank;
import mtymes.account.config.SystemProperties;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.dao.mongo.MongoAccountDao;
import mtymes.account.dao.mongo.MongoOperationDao;
import mtymes.api.BankApi;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.asynchttpclient.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;

import static de.flapdoodle.embed.process.runtime.Network.getFreeServerPort;
import static mtymes.account.dao.mongo.Collections.accountsCollection;
import static mtymes.account.dao.mongo.Collections.operationsCollection;

public class BankSystemTest {

    protected static EmbeddedDB db;
    protected static AccountDao accountDao;
    protected static OperationDao operationDao;

    @BeforeClass
    public static void initDB() {
        db = MongoManager.getEmbeddedDB();
        MongoDatabase database = db.getDatabase();

        accountDao = new MongoAccountDao(accountsCollection(database));
        operationDao = new MongoOperationDao(operationsCollection(database));
    }

    @AfterClass
    public static void releaseDB() {
        MongoManager.release(db);
    }

    @Test
    public void shouldFailToLoadNonExistingAccount() {
        // todo: implement
    }

    @Test
    public void shouldBeAbleToCreateAccount() {
        // todo: implement
    }

    @Test
    public void shouldBeAbleToDepositMoney() {
        // todo: implement
    }

    @Test
    public void shouldFailToDepositMoneyToNonExistingAccount() {
        // todo: implement
    }

    @Test
    public void shouldBeAbleToWithdrawMoney() {
        // todo: implement
    }

    @Test
    public void shouldBeAbleToWithdrawMoneyAndGetBalanceToZero() {
        // todo: implement
    }

    @Test
    public void shouldFailToWithdrawMoneyFromNonExistingAccount() {
        // todo: implement
    }

    @Test
    public void shouldFailToWithdrawMoneyIfAccountHasInsufficientFunds() {
        // todo: implement
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




    @Deprecated // todo: remove - make part of other system tests
    @Test
    public void shouldLoadAccount() throws Exception {
        SystemProperties properties = new SystemProperties(
                getFreeServerPort(),
                "localhost",
                db.getPort(),
                db.getDbName(),
                10,
                Duration.ofMillis(0)
        );
        Bank bank = new Bank(properties).start();

        BankApi api = new BankApi("localhost", bank.getPort());

        Response account = api.createAccount();
        System.out.println(account);
//        Response response = api.loadAccount(randomAccountId().toString());
//        System.out.println(response);
    }


}
