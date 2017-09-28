package mtymes.account.handler;

import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.common.util.SetQueue;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.account.domain.operation.FinalState.Rejected;
import static mtymes.test.ConcurrencyUtil.runConcurrentlyOnNThreads;
import static mtymes.test.OptionalMatcher.*;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TransferFromHandlerConcurrencyTest extends BaseOperationHandlerStabilityTest {

    private SetQueue<AccountId> workQueue = new SetQueue<>();
    private TransferFromHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new TransferFromHandler(accountDao, operationDao, workQueue);
    }

    @Test
    public void shouldSucceedToWithdrawMoneyAndTriggerTransferToOperationOnConcurrentExecution() {
        Decimal amount = randomPositiveAmount();

        Decimal fromBalance = pickRandomValue(amount, amount.plus(randomPositiveAmount()));
        Decimal toBalance = pickRandomValue(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount());
        AccountId fromAccountId = createAccountWithInitialBalance(fromBalance).accountId;
        Account toAccount = createAccountWithInitialBalance(toBalance);
        OperationId toPartOperationId = randomOperationId();
        TransferDetail detail = new TransferDetail(fromAccountId, toAccount.accountId, amount);

        TransferFrom transferFrom = new TransferFrom(randomOperationId(), toPartOperationId, detail);
        OpLogId opLogId = operationDao.storeOperation(transferFrom);

        // When
        runConcurrentlyOnNThreads(
                () -> handler.handleOperation(opLogId, transferFrom),
                50
        );

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Applied));
        assertThat(operation.description, isNotPresent());

        Account fromAccount = loadAccount(fromAccountId);
        assertThat(fromAccount, equalTo(new Account(fromAccountId, fromBalance.minus(amount), opLogId.seqId)));
        assertThat(loadAccount(toAccount.accountId), equalTo(toAccount));

        assertThat(workQueue.takeNextAvailable(), isPresentAndEqualTo(toAccount.accountId));
        assertThat(workQueue.takeNextAvailable(), isNotPresent());

        List<OpLogId> unfinishedOpLogIds = operationDao.findUnfinishedOperationLogIds(toAccount.accountId);
        assertThat(unfinishedOpLogIds.size(), is(1));
        Optional<LoggedOperation> loggedOperation = operationDao.findLoggedOperation(unfinishedOpLogIds.get(0));
        assertThat(loggedOperation, isPresent());
        assertThat(loggedOperation.get().operation, equalTo(new TransferTo(toPartOperationId, detail)));
    }

    @Test
    public void shouldFailToTransferFromIfThereIsInsufficientBalanceOnConcurrentExecution() {
        Decimal fromBalance = pickRandomValue(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount());
        Decimal toBalance = pickRandomValue(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount());
        Account initialFromAccount = createAccountWithInitialBalance(fromBalance);
        Account initialToAccount = createAccountWithInitialBalance(toBalance);
        AccountId fromAccountId = initialFromAccount.accountId;
        AccountId toAccountId = initialToAccount.accountId;

        Decimal amount = fromBalance.signum() >= 0 ? fromBalance.plus(randomPositiveAmount()) : randomPositiveAmount();
        OperationId toPartOperationId = randomOperationId();
        TransferDetail detail = new TransferDetail(fromAccountId, toAccountId, amount);

        TransferFrom transferFrom = new TransferFrom(randomOperationId(), toPartOperationId, detail);
        OpLogId opLogId = operationDao.storeOperation(transferFrom);

        // When
        Runnable task = () -> handler.handleOperation(opLogId, transferFrom);
        runConcurrentlyOnNThreads(task, 50);

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Rejected));
        assertThat(operation.description, isPresentAndEqualTo("Insufficient funds on account '" + fromAccountId + "'"));

        Account fromAccount = loadAccount(fromAccountId);
        assertThat(fromAccount, equalTo(new Account(fromAccountId, fromBalance, initialFromAccount.version)));
        Account toAccount = loadAccount(toAccountId);
        assertThat(toAccount, equalTo(new Account(toAccountId, toBalance, initialToAccount.version)));

        assertThat(workQueue.takeNextAvailable(), isNotPresent());

        List<OpLogId> unfinishedOpLogIds = operationDao.findUnfinishedOperationLogIds(toAccount.accountId);
        assertThat(unfinishedOpLogIds.size(), is(0));
    }
}