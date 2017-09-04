package mtymes.account.handler;

import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
import mtymes.test.ThreadSynchronizer;
import org.junit.Before;
import org.junit.Test;

import static javafixes.concurrency.Runner.runner;
import static mtymes.account.domain.operation.FinalState.Applied;
import static mtymes.test.OptionalMatcher.isNotPresent;
import static mtymes.test.OptionalMatcher.isPresentAndEqualTo;
import static mtymes.test.Random.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class TransferToHandlerConcurrencyTest extends BaseOperationHandlerStabilityTest {

    private TransferToHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new TransferToHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToDepositToOnConcurrentExecution() {
        int threadCount = 50;

        Decimal amount = randomPositiveAmount();

        Decimal initialBalance = pickRandomValue(randomNegativeAmount(), Decimal.ZERO, randomPositiveAmount());
        AccountId accountId = createAccountWithInitialBalance(initialBalance).accountId;

        TransferId transferId = randomTransferId();
        TransferTo transferTo = new TransferTo(new TransferDetail(
                transferId, randomAccountId(), accountId, amount
        ));
        OpLogId opLogId = operationDao.storeOperation(transferTo);

        // When
        Runner runner = runner(threadCount);
        ThreadSynchronizer synchronizer = new ThreadSynchronizer(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                synchronizer.blockUntilAllThreadsCallThisMethod();

                handler.handleOperation(opLogId, transferTo);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        LoggedOperation operation = loadOperation(opLogId);
        assertThat(operation.finalState, isPresentAndEqualTo(Applied));
        assertThat(operation.description, isNotPresent());
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance.plus(amount), opLogId.seqId)));
    }
}