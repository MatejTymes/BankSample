package mtymes.account.handler;

import com.mongodb.client.MongoDatabase;
import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.dao.AccountDao;
import mtymes.account.dao.OperationDao;
import mtymes.account.dao.mongo.MongoAccountDao;
import mtymes.account.dao.mongo.MongoOperationDao;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.test.db.EmbeddedDB;
import mtymes.test.db.MongoManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static java.lang.String.format;
import static javafixes.concurrency.Runner.runner;
import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.domain.operation.FinalState.Failure;
import static mtymes.account.domain.operation.FinalState.Success;
import static mtymes.account.mongo.Collections.accountsCollection;
import static mtymes.account.mongo.Collections.operationsCollection;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

// todo: move into test-stability
public class InternalTransferHandlerConcurrencyTest {

    private static EmbeddedDB db;
    private static AccountDao accountDao;
    private static OperationDao operationDao;
    private static InternalTransferHandler handler;

    @BeforeClass
    public static void initDB() {
        db = MongoManager.getEmbeddedDB();
        MongoDatabase database = db.getDatabase();

        accountDao = new MongoAccountDao(accountsCollection(database));
        operationDao = new MongoOperationDao(operationsCollection(database));
        handler = new InternalTransferHandler(accountDao, operationDao);
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
    public void shouldSuccessToTransferMoneyOnConcurrentExecution() {
        int threadCount = 50;

        Decimal amount = randomPositiveDecimal();

        Decimal fromBalance = amount.plus(randomPositiveDecimal());
        Decimal toBalance = pickRandomValue(randomNegativeDecimal(), Decimal.ZERO, randomPositiveDecimal());
        AccountId fromAccountId = createAccountWithInitialBalance(fromBalance).accountId;
        AccountId toAccountId = createAccountWithInitialBalance(toBalance).accountId;

        InternalTransfer internalTransfer = new InternalTransfer(fromAccountId, toAccountId, amount);
        OperationId operationId = operationDao.storeOperation(internalTransfer);

        // When
        Runner runner = runner(threadCount);
        CountDownLatch startSynchronizer = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startSynchronizer.countDown();
                startSynchronizer.await();

                handler.handleOperation(operationId, internalTransfer);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        PersistedOperation operation = loadOperation(operationId);
        assertThat(operation.finalState, isPresentAndEqualTo(Success));
        assertThat(operation.description, isNotPresent());
        Account fromAccount = loadAccount(fromAccountId);
        assertThat(fromAccount, equalTo(new Account(fromAccountId, fromBalance.minus(amount), operationId)));
        Account toAccount = loadAccount(toAccountId);
        assertThat(toAccount, equalTo(new Account(toAccountId, toBalance.plus(amount), operationId)));
    }

    @Test
    public void shouldFailToTransferMoneyOnConcurrentExecutionIfThereIsInsufficientBalance() {
        int threadCount = 50;

        Decimal fromBalance = pickRandomValue(randomNegativeDecimal(), Decimal.ZERO, randomPositiveDecimal());
        Decimal toBalance = pickRandomValue(randomNegativeDecimal(), Decimal.ZERO, randomPositiveDecimal());
        Account initialFromAccount = createAccountWithInitialBalance(fromBalance);
        Account initialToAccount = createAccountWithInitialBalance(toBalance);
        AccountId fromAccountId = initialFromAccount.accountId;
        AccountId toAccountId = initialToAccount.accountId;

        Decimal amount = fromBalance.signum() > 0 ? fromBalance.plus(randomPositiveDecimal()) : randomPositiveDecimal();
        InternalTransfer internalTransfer = new InternalTransfer(fromAccountId, toAccountId, amount);
        OperationId operationId = operationDao.storeOperation(internalTransfer);

        // When
        Runner runner = runner(threadCount);
        CountDownLatch startSynchronizer = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startSynchronizer.countDown();
                startSynchronizer.await();

                handler.handleOperation(operationId, internalTransfer);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        PersistedOperation operation = loadOperation(operationId);
        assertThat(operation.finalState, isPresentAndEqualTo(Failure));
        assertThat(operation.description, isPresentAndEqualTo("Insufficient funds on account '" + fromAccountId + "'"));
        Account fromAccount = loadAccount(fromAccountId);
        assertThat(fromAccount, equalTo(new Account(fromAccountId, fromBalance, initialFromAccount.lastAppliedOpId)));
        Account toAccount = loadAccount(toAccountId);
        assertThat(toAccount, equalTo(new Account(toAccountId, toBalance, initialToAccount.lastAppliedOpId)));
    }

    private Account createAccountWithInitialBalance(Decimal initialBalance) {
        AccountId accountId = newAccountId();
        OperationId operationId = operationDao.storeOperation(new CreateAccount(accountId));
        accountDao.createAccount(accountId, operationId);
        operationDao.markAsSuccessful(operationId);

        if (initialBalance.signum() > 0) {
            depositMoney(accountId, initialBalance);
        } else if (initialBalance.signum() < 0) {
            withdrawMoney(accountId, initialBalance.negate());
        }

        return loadAccount(accountId);
    }

    private void depositMoney(AccountId accountId, Decimal amount) {
        assertThat(amount.compareTo(Decimal.ZERO), greaterThan(0));

        Account account = loadAccount(accountId);
        OperationId operationId = operationDao.storeOperation(new DepositMoney(accountId, amount));

        accountDao.updateBalance(accountId, account.balance.plus(amount), account.lastAppliedOpId, operationId);

        operationDao.markAsSuccessful(operationId);
    }

    private void withdrawMoney(AccountId accountId, Decimal amount) {
        assertThat(amount.compareTo(Decimal.ZERO), greaterThan(0));

        Account account = loadAccount(accountId);
        OperationId operationId = operationDao.storeOperation(new DepositMoney(accountId, amount));

        accountDao.updateBalance(accountId, account.balance.minus(amount), account.lastAppliedOpId, operationId);

        operationDao.markAsSuccessful(operationId);
    }

    private Account loadAccount(AccountId accountId) {
        return accountDao.findAccount(accountId)
                .orElseThrow(() -> new IllegalStateException(format("Account '%s' should be present", accountId)));
    }

    private PersistedOperation loadOperation(OperationId operationId) {
        return operationDao.findOperation(operationId)
                .orElseThrow(() -> new IllegalStateException(format("Operation '%s' should be present", operationId)));
    }
}