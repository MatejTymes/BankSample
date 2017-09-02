package mtymes.account.handler;

import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.WorkQueue;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.LoggedOperation;
import mtymes.account.domain.operation.OpLogId;
import mtymes.account.domain.operation.TransferDetail;
import mtymes.account.domain.operation.TransferFrom;
import mtymes.test.ThreadSynchronizer;
import org.junit.Before;
import org.junit.Test;

import static javafixes.concurrency.Runner.runner;
import static mtymes.account.domain.operation.FinalState.Failure;
import static mtymes.account.domain.operation.FinalState.Success;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class TransferFromHandlerConcurrencyTest extends BaseOperationHandlerConcurrencyTest {

    private WorkQueue workQueue = new WorkQueue();
    private TransferFromHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new TransferFromHandler(accountDao, operationDao, workQueue);
    }

    @Test
    public void shouldSucceedToWithdrawMoneyAndTriggerTransferToOperationConcurrentExecution() {
        int threadCount = 50;

        Decimal amount = randomPositiveAmount();

        Decimal fromBalance = pickRandomValue(amount, amount.plus(randomPositiveAmount()));
        Decimal toBalance = pickRandomValue(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount());
        AccountId fromAccountId = createAccountWithInitialBalance(fromBalance).accountId;
        Account toAccount = createAccountWithInitialBalance(toBalance);
        TransferDetail detail = new TransferDetail(randomTransferId(), fromAccountId, toAccount.accountId, amount);

        TransferFrom transferFrom = new TransferFrom(detail);
        OpLogId opLogId = operationDao.storeOperation(transferFrom);

        // When
        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                synchronizer.blockUntilAllThreadsCallThisMethod();

                handler.handleOperation(opLogId, transferFrom);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Success));
        assertThat(operation.description, isNotPresent());

        Account fromAccount = loadAccount(fromAccountId);
        assertThat(fromAccount, equalTo(new Account(fromAccountId, fromBalance.minus(amount), opLogId.version)));
        assertThat(loadAccount(toAccount.accountId), equalTo(toAccount));

        assertThat(workQueue.takeNextAvailable(), isPresentAndEqualTo(toAccount.accountId));
        assertThat(workQueue.takeNextAvailable(), isNotPresent());

        // todo: verify TransferTo has been created
    }

    @Test
    public void shouldFailToTransferFromOnConcurrentExecutionIfThereIsInsufficientBalance() {
        int threadCount = 50;

        Decimal fromBalance = pickRandomValue(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount());
        Decimal toBalance = pickRandomValue(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount());
        Account initialFromAccount = createAccountWithInitialBalance(fromBalance);
        Account initialToAccount = createAccountWithInitialBalance(toBalance);
        AccountId fromAccountId = initialFromAccount.accountId;
        AccountId toAccountId = initialToAccount.accountId;

        Decimal amount = fromBalance.signum() >= 0 ? fromBalance.plus(randomPositiveAmount()) : randomPositiveAmount();
        TransferDetail detail = new TransferDetail(randomTransferId(), fromAccountId, toAccountId, amount);

        TransferFrom transferFrom = new TransferFrom(detail);
        OpLogId opLogId = operationDao.storeOperation(transferFrom);

        // When
        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                synchronizer.blockUntilAllThreadsCallThisMethod();

                handler.handleOperation(opLogId, transferFrom);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Failure));
        assertThat(operation.description, isPresentAndEqualTo("Insufficient funds on account '" + fromAccountId + "'"));

        Account fromAccount = loadAccount(fromAccountId);
        assertThat(fromAccount, equalTo(new Account(fromAccountId, fromBalance, initialFromAccount.version)));
        Account toAccount = loadAccount(toAccountId);
        assertThat(toAccount, equalTo(new Account(toAccountId, toBalance, initialToAccount.version)));

        assertThat(workQueue.takeNextAvailable(), isNotPresent());

        // todo: verify TransferTo has NOT been created
    }
}