package mtymes.account.handler;

import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.CreateAccount;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.PersistedOperation;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static javafixes.concurrency.Runner.runner;
import static mtymes.account.domain.account.AccountId.newAccountId;
import static mtymes.account.domain.operation.FinalState.Failure;
import static mtymes.account.domain.operation.FinalState.Success;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static org.junit.Assert.assertThat;

public class CreateAccountHandlerConcurrencyTest extends BaseOperationHandlerConcurrencyTest {

    private CreateAccountHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new CreateAccountHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToCreateAccountOnConcurrentExecution() {
        int threadCount = 50;
        Runner runner = runner(threadCount);

        AccountId accountId = newAccountId();
        CreateAccount createAccount = new CreateAccount(accountId);
        OperationId operationId = operationDao.storeOperation(createAccount);

        // When
        CountDownLatch startSynchronizer = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startSynchronizer.countDown();
                startSynchronizer.await();

                handler.handleOperation(operationId, createAccount);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        PersistedOperation operation = loadOperation(operationId);
        assertThat(operation.finalState, isPresentAndEqualTo(Success));
        assertThat(operation.description, isNotPresent());
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(new Account(accountId, Decimal.ZERO, operationId)));
    }

    @Test
    public void shouldFailToCreateAccountOnConcurrentExecutionIfItIsAlreadyPresent() {
        int threadCount = 50;
        Runner runner = runner(threadCount);

        AccountId accountId = newAccountId();
        Account initialAccount = createAccount(accountId);
        CreateAccount createAccount = new CreateAccount(accountId);
        OperationId operationId = operationDao.storeOperation(createAccount);

        // When
        CountDownLatch startSynchronizer = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startSynchronizer.countDown();
                startSynchronizer.await();

                handler.handleOperation(operationId, createAccount);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        PersistedOperation operation = loadOperation(operationId);
        assertThat(operation.finalState, isPresentAndEqualTo(Failure));
        assertThat(operation.description, isPresentAndEqualTo("Account '" + accountId + "' already exists"));
        Optional<Account> account = accountDao.findAccount(accountId);
        assertThat(account, isPresentAndEqualTo(initialAccount));
    }
}