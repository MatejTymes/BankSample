package mtymes.account.handler;

import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.*;
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

public class TransferMoneyToHandlerConcurrencyTest extends BaseOperationHandlerConcurrencyTest {

    private TransferMoneyToHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new TransferMoneyToHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToDepositMoneyOnConcurrentExecution() {
        int threadCount = 50;

        Decimal amount = randomPositiveDecimal();

        Decimal initialBalance = pickRandomValue(randomNegativeDecimal(), Decimal.ZERO, randomPositiveDecimal());
        AccountId accountId = createAccountWithInitialBalance(initialBalance).accountId;

        TransferId transferId = randomTransferId();
        TransferMoneyTo transferMoneyTo = new TransferMoneyTo(new TransferDetail(
                transferId, randomAccountId(), accountId, amount
        ));
        SeqId seqId = operationDao.storeOperation(transferMoneyTo);

        // When
        Runner runner = runner(threadCount);
        CountDownLatch startSynchronizer = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startSynchronizer.countDown();
                startSynchronizer.await();

                handler.handleOperation(seqId, transferMoneyTo);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        PersistedOperation operation = loadOperation(seqId);
        assertThat(operation.finalState, isPresentAndEqualTo(Success));
        assertThat(operation.description, isNotPresent());
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance.plus(amount), seqId)));
    }
}