package mtymes.account.dao.mongo;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.SeqId;
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
        SeqId seqId = randomOperationId();

        // When
        boolean success = accountDao.createAccount(accountId, seqId);

        // Then
        assertThat(success, is(true));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, ZERO, seqId)));
    }

    @Test
    public void shouldFailToCreateAccountIfItAlreadyExists() {
        AccountId accountId = newAccountId();
        SeqId seqId = randomOperationId();
        accountDao.createAccount(accountId, seqId);

        SeqId newSeqId = randomOperationId(otherThan(seqId));

        // When
        boolean success = accountDao.createAccount(accountId, newSeqId);

        // Then
        assertThat(success, is(false));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, ZERO, seqId)));
    }

    @Test
    public void shouldNotFindNonExistingAccount() {
        assertThat(accountDao.findAccount(randomAccountId()), isNotPresent());
    }

    @Test
    public void shouldUpdateBalance() {
        AccountId accountId = newAccountId();
        SeqId lastSeqId = randomOperationId();
        accountDao.createAccount(accountId, lastSeqId);

        SeqId newSeqId = randomOperationId(otherThan(lastSeqId));
        Decimal newBalance = randomDecimal();

        // When
        boolean success = accountDao.updateBalance(accountId, newBalance, lastSeqId, newSeqId);

        // Then
        assertThat(success, is(true));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, newBalance, newSeqId)));
    }

    @Test
    public void shouldNotUpdateBalanceOnLastOperationIdMismatch() {
        AccountId accountId = newAccountId();
        SeqId lastSeqId = randomOperationId();
        accountDao.createAccount(accountId, lastSeqId);

        SeqId differentLastSeqId = randomOperationId(otherThan(lastSeqId)) ;
        SeqId newSeqId = randomOperationId(otherThan(lastSeqId, differentLastSeqId));
        Decimal newBalance = randomDecimal();

        // When
        boolean success = accountDao.updateBalance(accountId, newBalance, differentLastSeqId, newSeqId);

        // Then
        assertThat(success, is(false));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, Decimal.ZERO, lastSeqId)));
    }

    @Test
    public void shouldNotUpdateBalanceForNonExistingAccount() {
        assertThat(accountDao.updateBalance(randomAccountId(), randomDecimal(), randomOperationId(), randomOperationId()), is(false));
    }
}