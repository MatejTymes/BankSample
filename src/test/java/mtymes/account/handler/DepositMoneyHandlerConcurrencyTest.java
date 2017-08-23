package mtymes.account.handler;

import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.DepositMoney;
import mtymes.account.domain.operation.OperationId;
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

public class DepositMoneyHandlerConcurrencyTest extends BaseOperationHandlerConcurrencyTest {

    private DepositMoneyHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new DepositMoneyHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToDepositMoneyOnConcurrentExecution() {
        int threadCount = 50;

        Decimal amount = randomPositiveDecimal();

        Decimal initialBalance = pickRandomValue(randomNegativeDecimal(), Decimal.ZERO, randomPositiveDecimal());
        AccountId accountId = createAccountWithInitialBalance(initialBalance).accountId;

        DepositMoney depositMoney = new DepositMoney(accountId, amount);
        OperationId operationId = operationDao.storeOperation(depositMoney);

        // When
        Runner runner = runner(threadCount);
        CountDownLatch startSynchronizer = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startSynchronizer.countDown();
                startSynchronizer.await();

                handler.handleOperation(operationId, depositMoney);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        PersistedOperation operation = loadOperation(operationId);
        assertThat(operation.finalState, isPresentAndEqualTo(Success));
        assertThat(operation.description, isNotPresent());
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance.plus(amount), operationId)));
    }
}