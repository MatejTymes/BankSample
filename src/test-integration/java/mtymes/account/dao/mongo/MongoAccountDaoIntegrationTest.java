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
import static mtymes.account.dao.mongo.MongoCollections.accountsCollection;
import static mtymes.account.domain.operation.SeqId.seqId;
import static mtymes.test.Condition.after;
import static mtymes.test.Condition.otherThan;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
        AccountId accountId = randomAccountId();
        SeqId version = randomSeqId();

        // When
        boolean success = accountDao.createAccount(accountId, version);

        // Then
        assertThat(success, is(true));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, ZERO, version)));
        assertThat(accountDao.findCurrentVersion(accountId), isPresentAndEqualTo(version));
    }

    @Test
    public void shouldFailToCreateAccountIfItAlreadyExists() {
        AccountId accountId = randomAccountId();
        SeqId version = randomSeqId();
        accountDao.createAccount(accountId, version);

        SeqId newVersion = randomSeqId(otherThan(version));

        // When
        boolean success = accountDao.createAccount(accountId, newVersion);

        // Then
        assertThat(success, is(false));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, ZERO, version)));
        assertThat(accountDao.findCurrentVersion(accountId), isPresentAndEqualTo(version));
    }

    @Test
    public void shouldNotFindNonExistingAccount() {
        assertThat(accountDao.findAccount(randomAccountId()), isNotPresent());
        assertThat(accountDao.findCurrentVersion(randomAccountId()), isNotPresent());
    }

    @Test
    public void shouldUpdateBalance() {
        AccountId accountId = randomAccountId();
        SeqId currentVersion = randomSeqId();
        accountDao.createAccount(accountId, currentVersion);

        SeqId newVersion = seqId(currentVersion.value() + randomLong(1, 100));
        Decimal newBalance = randomAmount();

        // When
        boolean success = accountDao.updateBalance(accountId, newBalance, currentVersion, newVersion);

        // Then
        assertThat(success, is(true));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, newBalance, newVersion)));
        assertThat(accountDao.findCurrentVersion(accountId), isPresentAndEqualTo(newVersion));
    }

    @Test
    public void shouldNotUpdateBalanceOnVersionMismatch() {
        AccountId accountId = randomAccountId();
        SeqId currentVersion = randomSeqId();
        accountDao.createAccount(accountId, currentVersion);

        SeqId differentVersion = seqId(currentVersion.value() + randomLong(1, 100));
        SeqId newVersion = seqId(differentVersion.value() + randomLong(1, 100));
        Decimal newBalance = randomAmount();

        // When
        boolean success = accountDao.updateBalance(accountId, newBalance, differentVersion, newVersion);

        // Then
        assertThat(success, is(false));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, Decimal.ZERO, currentVersion)));
        assertThat(accountDao.findCurrentVersion(accountId), isPresentAndEqualTo(currentVersion));
    }

    @Test
    public void shouldFailToUpdateBalanceIfNewVersionIsBeforeCurrentVersion() {
        AccountId accountId = randomAccountId();
        SeqId currentVersion = randomSeqId();
        accountDao.createAccount(accountId, currentVersion);

        SeqId newVersion = seqId(currentVersion.value() - randomLong(1, 100));
        Decimal newBalance = randomAmount();

        try {
            // When
            accountDao.updateBalance(accountId, newBalance, currentVersion, newVersion);
            // Then
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("oldVersion must be before newVersion"));
        }
    }

    @Test
    public void shouldFailToUpdateBalanceIfNewVersionIsTheSameCurrentVersion() {
        AccountId accountId = randomAccountId();
        SeqId currentVersion = randomSeqId();
        accountDao.createAccount(accountId, currentVersion);

        SeqId newVersion = currentVersion;
        Decimal newBalance = randomAmount();

        try {
            // When
            accountDao.updateBalance(accountId, newBalance, currentVersion, newVersion);
            // Then
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), equalTo("oldVersion must be before newVersion"));
        }
    }

    @Test
    public void shouldNotUpdateBalanceForNonExistingAccount() {
        SeqId oldVersion = randomSeqId();
        SeqId newVersion = randomSeqId(after(oldVersion));
        assertThat(accountDao.updateBalance(randomAccountId(), randomAmount(), oldVersion, newVersion), is(false));
    }
}