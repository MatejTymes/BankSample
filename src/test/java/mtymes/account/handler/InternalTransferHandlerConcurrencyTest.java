package mtymes.account.handler;

import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.InternalTransfer;
import mtymes.account.domain.operation.OperationId;
import mtymes.account.domain.operation.PersistedOperation;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static javafixes.concurrency.Runner.runner;
import static mtymes.account.domain.operation.FinalState.Failure;
import static mtymes.account.domain.operation.FinalState.Success;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

// todo: move into test-stability
public class InternalTransferHandlerConcurrencyTest extends BaseOperationHandlerConcurrencyTest {

    private InternalTransferHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new InternalTransferHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToTransferMoneyOnConcurrentExecution() {
        int threadCount = 50;

        Decimal amount = randomPositiveDecimal();

        Decimal fromBalance = pickRandomValue(amount, amount.plus(randomPositiveDecimal()));
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

        Decimal amount = fromBalance.signum() >= 0 ? fromBalance.plus(randomPositiveDecimal()) : randomPositiveDecimal();
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
}