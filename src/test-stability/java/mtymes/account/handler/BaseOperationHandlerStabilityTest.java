package mtymes.account.handler;

import com.mongodb.client.MongoDatabase;
import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OpLogDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.dao.mongo.MongoAccountDao;
import mtymes.account.dao.mongo.MongoOpLogDao;
import mtymes.account.dao.mongo.MongoOperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static java.lang.String.format;
import static mtymes.account.dao.mongo.MongoCollections.*;
import static mtymes.test.Random.randomAccountId;
import static mtymes.test.Random.randomOperationId;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public abstract class BaseOperationHandlerStabilityTest {

    protected static EmbeddedDB db;
    protected static AccountDao accountDao;
    protected static OperationDao operationDao;
    protected static OpLogDao opLogDao;

    @BeforeClass
    public static void initDB() {
        db = MongoManager.getEmbeddedDB();
        MongoDatabase database = db.getDatabase();

        accountDao = new MongoAccountDao(accountsCollection(database));
        operationDao = new MongoOperationDao(operationsCollection(database));
        opLogDao = new MongoOpLogDao(opLogCollection(database));
    }

    @AfterClass
    public static void releaseDB() {
        MongoManager.release(db);
    }

    protected Account createAccountWithInitialBalance(Decimal initialBalance) {
        AccountId accountId = randomAccountId();
        createAccount(accountId);
        if (initialBalance.signum() > 0) {
            depositMoney(accountId, initialBalance);
        } else if (initialBalance.signum() < 0) {
            withdrawMoney(accountId, initialBalance.negate());
        }

        return loadAccount(accountId);
    }

    protected Account createAccount(AccountId accountId) {
        OperationId operationId = randomOperationId();
        operationDao.storeOperation(new CreateAccount(operationId, accountId));
        SeqId seqId = opLogDao.registerOperationId(accountId, operationId);

        accountDao.createAccount(accountId, seqId);

        operationDao.markAsApplied(operationId);
        opLogDao.markAsFinished(operationId);
        return loadAccount(accountId);
    }

    protected void depositMoney(AccountId accountId, Decimal amount) {
        assertThat(amount.compareTo(Decimal.ZERO), greaterThan(0));

        Account account = loadAccount(accountId);
        OperationId operationId = randomOperationId();
        operationDao.storeOperation(new DepositTo(operationId, accountId, amount));
        SeqId seqId = opLogDao.registerOperationId(accountId, operationId);

        accountDao.updateBalance(accountId, account.balance.plus(amount), account.version, seqId);

        operationDao.markAsApplied(operationId);
        opLogDao.markAsFinished(operationId);
    }

    protected void withdrawMoney(AccountId accountId, Decimal amount) {
        assertThat(amount.compareTo(Decimal.ZERO), greaterThan(0));

        Account account = loadAccount(accountId);
        OperationId operationId = randomOperationId();
        operationDao.storeOperation(new WithdrawFrom(operationId, accountId, amount));
        SeqId seqId = opLogDao.registerOperationId(accountId, operationId);

        accountDao.updateBalance(accountId, account.balance.minus(amount), account.version, seqId);

        operationDao.markAsApplied(operationId);
        opLogDao.markAsFinished(operationId);
    }

    protected Account loadAccount(AccountId accountId) {
        return accountDao.findAccount(accountId)
                .orElseThrow(() -> new IllegalStateException(format("Account '%s' should be present", accountId)));
    }

    protected LoggedOperation loadOperation(OperationId operationId) {
        return operationDao.findLoggedOperation(operationId)
                .orElseThrow(() -> new IllegalStateException(format("Operation '%s' should be present", operationId)));
    }
}
