package mtymes.account.dao;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.test.db.EmbeddedDB;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.mongo.Collections.accountsCollection;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.randomAccountId;
import static mtymes.test.Random.randomOperationId;
import static mtymes.test.db.EmbeddedDB.embeddedDB;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

// todo: move into test-integration
public class AccountDaoTest {

    private static EmbeddedDB db;
    private static AccountDao accountDao;

    @BeforeClass
    public static void initDB() {
        db = embeddedDB().start();
        accountDao = new AccountDao(accountsCollection(db.getDatabase()));
    }

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
    }

    @AfterClass
    public static void releaseDB() {
        db.stop();
    }

    @Test
    public void shouldLoadNoAccountOnUnknownAccountId() {
        assertThat(accountDao.findAccount(randomAccountId()), isNotPresent());
    }

    @Test
    public void shouldCreateAndLoadNewAccount() {
        OperationId operationId = randomOperationId();
        AccountId accountId = newAccountId();

        // When
        boolean success = accountDao.createAccount(accountId, operationId);

        // Then
        assertThat(success, is(true));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, Decimal.ZERO, operationId)));
    }

}