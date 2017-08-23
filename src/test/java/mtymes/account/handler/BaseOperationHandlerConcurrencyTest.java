package mtymes.account.handler;

import com.mongodb.client.MongoDatabase;
import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.dao.mongo.MongoAccountDao;
import mtymes.account.dao.mongo.MongoOperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.DepositMoney;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.PersistedOperation;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static java.lang.String.format;
import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.mongo.Collections.accountsCollection;
import static mtymes.account.mongo.Collections.operationsCollection;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public abstract class BaseOperationHandlerConcurrencyTest {

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

    protected Account createAccountWithInitialBalance(Decimal initialBalance) {
        AccountId accountId = newAccountId();
        createAccount(accountId);
        if (initialBalance.signum() > 0) {
            depositMoney(accountId, initialBalance);
        } else if (initialBalance.signum() < 0) {
            withdrawMoney(accountId, initialBalance.negate());
        }

        return loadAccount(accountId);
    }

    protected Account createAccount(AccountId accountId) {
        OperationId operationId = operationDao.storeOperation(new CreateAccount(accountId));
        accountDao.createAccount(accountId, operationId);
        operationDao.markAsSuccessful(operationId);
        return loadAccount(accountId);
    }

    protected void depositMoney(AccountId accountId, Decimal amount) {
        assertThat(amount.compareTo(Decimal.ZERO), greaterThan(0));

        Account account = loadAccount(accountId);
        OperationId operationId = operationDao.storeOperation(new DepositMoney(accountId, amount));

        accountDao.updateBalance(accountId, account.balance.plus(amount), account.lastAppliedOpId, operationId);

        operationDao.markAsSuccessful(operationId);
    }

    protected void withdrawMoney(AccountId accountId, Decimal amount) {
        assertThat(amount.compareTo(Decimal.ZERO), greaterThan(0));

        Account account = loadAccount(accountId);
        OperationId operationId = operationDao.storeOperation(new DepositMoney(accountId, amount));

        accountDao.updateBalance(accountId, account.balance.minus(amount), account.lastAppliedOpId, operationId);

        operationDao.markAsSuccessful(operationId);
    }

    protected Account loadAccount(AccountId accountId) {
        return accountDao.findAccount(accountId)
                .orElseThrow(() -> new IllegalStateException(format("Account '%s' should be present", accountId)));
    }

    protected PersistedOperation loadOperation(OperationId operationId) {
        return operationDao.findOperation(operationId)
                .orElseThrow(() -> new IllegalStateException(format("Operation '%s' should be present", operationId)));
    }
}
