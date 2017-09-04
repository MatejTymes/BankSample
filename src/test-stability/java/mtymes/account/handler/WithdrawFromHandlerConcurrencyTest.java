package mtymes.account.handler;

import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.WithdrawFrom;
import mtymes.test.ThreadSynchronizer;
import org.junit.Before;
import org.junit.Test;

import static javafixes.concurrency.Runner.runner;
import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class WithdrawFromHandlerConcurrencyTest extends BaseOperationHandlerStabilityTest {

    private WithdrawFromHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new WithdrawFromHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToWithdrawFromOnConcurrentExecution() {
        int threadCount = 50;

        Decimal amount = randomPositiveAmount();

        Decimal initialBalance = pickRandomValue(amount, amount.plus(randomPositiveAmount()));
        AccountId accountId = createAccountWithInitialBalance(initialBalance).accountId;

        WithdrawFrom withdrawFrom = new WithdrawFrom(accountId, amount);
        OpLogId opLogId = operationDao.storeOperation(withdrawFrom);

        // When
        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                synchronizer.blockUntilAllThreadsCallThisMethod();

                handler.handleOperation(opLogId, withdrawFrom);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Applied));
        assertThat(operation.description, isNotPresent());
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance.minus(amount), opLogId.seqId)));
    }

    @Test
    public void shouldFailToWithdrawFromOnConcurrentExecutionIfThereIsInsufficientBalance() {
        int threadCount = 50;

        Decimal initialBalance = pickRandomValue(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount());
        Account initialAccount = createAccountWithInitialBalance(initialBalance);
        AccountId accountId = initialAccount.accountId;

        Decimal amount = initialBalance.signum() >= 0 ? initialBalance.plus(randomPositiveAmount()) : randomPositiveAmount();
        WithdrawFrom withdrawFrom = new WithdrawFrom(accountId, amount);
        OpLogId opLogId = operationDao.storeOperation(withdrawFrom);

        // When
        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                synchronizer.blockUntilAllThreadsCallThisMethod();

                handler.handleOperation(opLogId, withdrawFrom);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Rejected));
        assertThat(operation.description, isPresentAndEqualTo("Insufficient funds on account '" + accountId + "'"));
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance, initialAccount.version)));
    }
}