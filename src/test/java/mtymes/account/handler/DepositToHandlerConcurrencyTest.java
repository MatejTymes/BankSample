package mtymes.account.handler;

import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.DepositTo;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.PersistedOperation;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static javafixes.concurrency.Runner.runner;
import static mtymes.account.domain.operation.FinalState.Success;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class DepositToHandlerConcurrencyTest extends BaseOperationHandlerConcurrencyTest {

    private DepositToHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new DepositToHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToDepositToOnConcurrentExecution() {
        int threadCount = 50;

        Decimal amount = randomPositiveDecimal();

        Decimal initialBalance = pickRandomValue(randomNegativeDecimal(), Decimal.ZERO, randomPositiveDecimal());
        AccountId accountId = createAccountWithInitialBalance(initialBalance).accountId;

        DepositTo depositTo = new DepositTo(accountId, amount);
        OpLogId opLogId = operationDao.storeOperation(depositTo);

        // When
        Runner runner = runner(threadCount);
        CountDownLatch startSynchronizer = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startSynchronizer.countDown();
                startSynchronizer.await();

                handler.handleOperation(opLogId, depositTo);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        PersistedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Success));
        assertThat(operation.description, isNotPresent());
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance.plus(amount), opLogId.version)));
    }
}