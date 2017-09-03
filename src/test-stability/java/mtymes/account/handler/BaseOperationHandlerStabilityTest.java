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
import mtymes.account.domain.operation.DepositTo;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static java.lang.String.format;
import static mtymes.account.dao.mongo.Collections.accountsCollection;
import static mtymes.account.dao.mongo.Collections.operationsCollection;
import static mtymes.account.domain.account.AccountId.newAccountId;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public abstract class BaseOperationHandlerStabilityTest {

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
        OpLogId opLogId = operationDao.storeOperation(new CreateAccount(accountId));
        accountDao.createAccount(accountId, opLogId.version);
        operationDao.markAsSuccessful(opLogId);
        return loadAccount(accountId);
    }

    protected void depositMoney(AccountId accountId, Decimal amount) {
        assertThat(amount.compareTo(Decimal.ZERO), greaterThan(0));

        Account account = loadAccount(accountId);
        OpLogId opLogId = operationDao.storeOperation(new DepositTo(accountId, amount));

        accountDao.updateBalance(accountId, account.balance.plus(amount), account.version, opLogId.version);

        operationDao.markAsSuccessful(opLogId);
    }

    protected void withdrawMoney(AccountId accountId, Decimal amount) {
        assertThat(amount.compareTo(Decimal.ZERO), greaterThan(0));

        Account account = loadAccount(accountId);
        OpLogId opLogId = operationDao.storeOperation(new DepositTo(accountId, amount));

        accountDao.updateBalance(accountId, account.balance.minus(amount), account.version, opLogId.version);

        operationDao.markAsSuccessful(opLogId);
    }

    protected Account loadAccount(AccountId accountId) {
        return accountDao.findAccount(accountId)
                .orElseThrow(() -> new IllegalStateException(format("Account '%s' should be present", accountId)));
    }

    protected LoggedOperation loadOperation(OpLogId opLogId) {
        return operationDao.findLoggedOperation(opLogId)
                .orElseThrow(() -> new IllegalStateException(format("Operation '%s' should be present", opLogId)));
    }
}
