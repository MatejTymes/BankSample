package mtymes.account.handler;

import javafixes.concurrency.Runner;
import javafixes.math.Decimal;
import mtymes.account.domain.account.Account;
import mtymes.account.domain.account.AccountId;
import mtymes.account.domain.operation.PersistedOperation;
import mtymes.account.domain.operation.SeqId;
import mtymes.account.domain.operation.WithdrawMoney;
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

public class WithdrawMoneyHandlerConcurrencyTest extends BaseOperationHandlerConcurrencyTest {

    private WithdrawMoneyHandler handler;

    @Before
    public void setUp() throws Exception {
        db.removeAllData();
        handler = new WithdrawMoneyHandler(accountDao, operationDao);
    }

    @Test
    public void shouldSucceedToWithdrawMoneyOnConcurrentExecution() {
        int threadCount = 50;

        Decimal amount = randomPositiveDecimal();

        Decimal initialBalance = pickRandomValue(amount, amount.plus(randomPositiveDecimal()));
        AccountId accountId = createAccountWithInitialBalance(initialBalance).accountId;

        WithdrawMoney withdrawMoney = new WithdrawMoney(accountId, amount);
        SeqId seqId = operationDao.storeOperation(withdrawMoney);

        // When
        Runner runner = runner(threadCount);
        CountDownLatch startSynchronizer = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startSynchronizer.countDown();
                startSynchronizer.await();

                handler.handleOperation(seqId, withdrawMoney);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        PersistedOperation operation = loadOperation(seqId);
        assertThat(operation.finalState, isPresentAndEqualTo(Success));
        assertThat(operation.description, isNotPresent());
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance.minus(amount), seqId)));
    }

    @Test
    public void shouldFailToWithdrawMoneyOnConcurrentExecutionIfThereIsInsufficientBalance() {
        int threadCount = 50;

        Decimal initialBalance = pickRandomValue(randomNegativeDecimal(), Decimal.ZERO, randomPositiveDecimal());
        Account initialAccount = createAccountWithInitialBalance(initialBalance);
        AccountId accountId = initialAccount.accountId;

        Decimal amount = initialBalance.signum() >= 0 ? initialBalance.plus(randomPositiveDecimal()) : randomPositiveDecimal();
        WithdrawMoney withdrawMoney = new WithdrawMoney(accountId, amount);
        SeqId seqId = operationDao.storeOperation(withdrawMoney);

        // When
        Runner runner = runner(threadCount);
        CountDownLatch startSynchronizer = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            runner.runTask(() -> {
                startSynchronizer.countDown();
                startSynchronizer.await();

                handler.handleOperation(seqId, withdrawMoney);
            });
        }
        runner.waitTillDone().shutdown();

        // Then
        PersistedOperation operation = loadOperation(seqId);
        assertThat(operation.finalState, isPresentAndEqualTo(Failure));
        assertThat(operation.description, isPresentAndEqualTo("Insufficient funds on account '" + accountId + "'"));
        Account account = loadAccount(accountId);
        assertThat(account, equalTo(new Account(accountId, initialBalance, initialAccount.lastAppliedOpSeqId)));
    }
}