package mtymes.account.dao;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.OperationId;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static javafixes.math.Decimal.ZERO;
import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.mongo.Collections.accountsCollection;
import static mtymes.test.Condition.otherThan;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

// todo: move into test-integration
public class MongoAccountDaoIntegrationTest {

    private static EmbeddedDB db;
    private static AccountDao accountDao;

    @BeforeClass
    public static void initDB() {
        db = MongoManager.getEmbeddedDB();
        accountDao = new MongoAccountDao(accountsCollection(db.getDatabase()));
    }

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
    }

    @AfterClass
    public static void releaseDB() {
        MongoManager.release(db);
    }

    @Test
    public void shouldCreateAndLoadNewAccount() {
        AccountId accountId = newAccountId();
        OperationId operationId = randomOperationId();

        // When
        boolean success = accountDao.createAccount(accountId, operationId);

        // Then
        assertThat(success, is(true));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, ZERO, operationId)));
    }

    @Test
    public void shouldFailToCreateAccountIfItAlreadyExists() {
        AccountId accountId = newAccountId();
        OperationId operationId = randomOperationId();
        accountDao.createAccount(accountId, operationId);

        OperationId newOperationId = randomOperationId(otherThan(operationId));

        // When
        boolean success = accountDao.createAccount(accountId, newOperationId);

        // Then
        assertThat(success, is(false));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, ZERO, operationId)));
    }

    @Test
    public void shouldNotFindNonExistingAccount() {
        assertThat(accountDao.findAccount(randomAccountId()), isNotPresent());
    }

    @Test
    public void shouldUpdateBalance() {
        AccountId accountId = newAccountId();
        OperationId lastOperationId = randomOperationId();
        accountDao.createAccount(accountId, lastOperationId);

        OperationId newOperationId = randomOperationId(otherThan(lastOperationId));
        Decimal newBalance = randomDecimal();

        // When
        boolean success = accountDao.updateBalance(accountId, newBalance, lastOperationId, newOperationId);

        // Then
        assertThat(success, is(true));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, newBalance, newOperationId)));
    }

    @Test
    public void shouldNotUpdateBalanceOnOperationIdMismatch() {
        AccountId accountId = newAccountId();
        OperationId lastOperationId = randomOperationId();
        accountDao.createAccount(accountId, lastOperationId);

        OperationId differentLastOperationId = randomOperationId(otherThan(lastOperationId)) ;
        OperationId newOperationId = randomOperationId(otherThan(lastOperationId, differentLastOperationId));
        Decimal newBalance = randomDecimal();

        // When
        boolean success = accountDao.updateBalance(accountId, newBalance, differentLastOperationId, newOperationId);

        // Then
        assertThat(success, is(false));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, Decimal.ZERO, lastOperationId)));
    }

    @Test
    public void shouldNotUpdateBalanceForNonExistingAccount() {
        assertThat(accountDao.updateBalance(randomAccountId(), randomDecimal(), randomOperationId(), randomOperationId()), is(false));
    }
}