package mtymes.account.dao.mongo;

import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.Version;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static javafixes.math.Decimal.ZERO;
import static mtymes.account.dao.mongo.Collections.accountsCollection;
import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.domain.operation.Version.version;
import static mtymes.test.Condition.after;
import static mtymes.test.Condition.otherThan;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
        Version version = randomVersion();

        // When
        boolean success = accountDao.createAccount(accountId, version);

        // Then
        assertThat(success, is(true));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, ZERO, version)));
        assertThat(accountDao.findVersion(accountId), isPresentAndEqualTo(version));
    }

    @Test
    public void shouldFailToCreateAccountIfItAlreadyExists() {
        AccountId accountId = newAccountId();
        Version version = randomVersion();
        accountDao.createAccount(accountId, version);

        Version newVersion = randomVersion(otherThan(version));

        // When
        boolean success = accountDao.createAccount(accountId, newVersion);

        // Then
        assertThat(success, is(false));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, ZERO, version)));
        assertThat(accountDao.findVersion(accountId), isPresentAndEqualTo(version));
    }

    @Test
    public void shouldNotFindNonExistingAccount() {
        assertThat(accountDao.findAccount(randomAccountId()), isNotPresent());
        assertThat(accountDao.findVersion(randomAccountId()), isNotPresent());
    }

    @Test
    public void shouldUpdateBalance() {
        AccountId accountId = newAccountId();
        Version currentVersion = randomVersion();
        accountDao.createAccount(accountId, currentVersion);

        Version newVersion = version(currentVersion.value() + randomLong(1, 100));
        Decimal newBalance = randomDecimal();

        // When
        boolean success = accountDao.updateBalance(accountId, newBalance, currentVersion, newVersion);

        // Then
        assertThat(success, is(true));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, newBalance, newVersion)));
        assertThat(accountDao.findVersion(accountId), isPresentAndEqualTo(newVersion));
    }

    @Test
    public void shouldNotUpdateBalanceOnVersionMismatch() {
        AccountId accountId = newAccountId();
        Version currentVersion = randomVersion();
        accountDao.createAccount(accountId, currentVersion);

        Version differentVersion = version(currentVersion.value() + randomLong(1, 100));
        Version newVersion = version(differentVersion.value() + randomLong(1, 100));
        Decimal newBalance = randomDecimal();

        // When
        boolean success = accountDao.updateBalance(accountId, newBalance, differentVersion, newVersion);

        // Then
        assertThat(success, is(false));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, Decimal.ZERO, currentVersion)));
        assertThat(accountDao.findVersion(accountId), isPresentAndEqualTo(currentVersion));
    }

    @Test
    public void shouldFailToUpdateBalanceIfNewVersionIsBeforeCurrentVersion() {
        AccountId accountId = newAccountId();
        Version currentVersion = randomVersion();
        accountDao.createAccount(accountId, currentVersion);

        Version newVersion = version(currentVersion.value() - randomLong(1, 100));
        Decimal newBalance = randomDecimal();

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
        AccountId accountId = newAccountId();
        Version currentVersion = randomVersion();
        accountDao.createAccount(accountId, currentVersion);

        Version newVersion = currentVersion;
        Decimal newBalance = randomDecimal();

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
        Version oldVersion = randomVersion();
        Version newVersion = randomVersion(after(oldVersion));
        assertThat(accountDao.updateBalance(randomAccountId(), randomDecimal(), oldVersion, newVersion), is(false));
    }
}